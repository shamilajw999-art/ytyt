-- ====================================================================
-- MYTHIC SUBSCRIPTIONS & BANK TRANSFER APPROVAL ENGINE
-- Run this in your Supabase SQL Editor (https://supabase.com/dashboard)
-- ====================================================================

-- 1. Create Payment Orders Table
CREATE TABLE IF NOT EXISTS public.payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    user_email TEXT,
    plan TEXT NOT NULL CHECK (plan IN ('pro', 'max', 'prime')),
    amount NUMERIC(10, 2) NOT NULL,
    currency TEXT DEFAULT 'USD',
    payment_reference TEXT UNIQUE NOT NULL,
    bank_transaction_id TEXT,
    receipt_image_url TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'submitted', 'verified', 'approved', 'rejected', 'expired')),
    created_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ DEFAULT (now() + INTERVAL '7 days'),
    verified_at TIMESTAMPTZ,
    verified_by TEXT,
    rejection_reason TEXT,
    subscription_id UUID
);

-- 2. Create Subscriptions Table
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plan TEXT NOT NULL CHECK (plan IN ('pro', 'max', 'prime')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'expired', 'cancelled')),
    payment_order_id UUID REFERENCES public.payment_orders(id) ON DELETE SET NULL,
    started_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Ensure profiles table has subscription_tier column
ALTER TABLE IF EXISTS public.profiles 
ADD COLUMN IF NOT EXISTS subscription_tier TEXT DEFAULT 'free';

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.payment_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;

-- 5. Payment Orders RLS Policies
DROP POLICY IF EXISTS "Users can view own payment orders" ON public.payment_orders;
CREATE POLICY "Users can view own payment orders" 
ON public.payment_orders FOR SELECT 
TO authenticated 
USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can create own payment orders" ON public.payment_orders;
CREATE POLICY "Users can create own payment orders" 
ON public.payment_orders FOR INSERT 
TO authenticated 
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own payment orders" ON public.payment_orders;
CREATE POLICY "Users can update own payment orders" 
ON public.payment_orders FOR UPDATE 
TO authenticated 
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Service role / Admin full access to payment orders" ON public.payment_orders;
CREATE POLICY "Service role / Admin full access to payment orders" 
ON public.payment_orders FOR ALL 
TO authenticated 
USING (
  auth.uid() = user_id 
  OR (auth.jwt() ->> 'email') IN ('shamilajw999@gmail.com')
  OR current_user = 'service_role'
);

-- 6. Subscriptions RLS Policies
DROP POLICY IF EXISTS "Users can view own subscriptions" ON public.subscriptions;
CREATE POLICY "Users can view own subscriptions" 
ON public.subscriptions FOR SELECT 
TO authenticated 
USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Service role / Admin full access to subscriptions" ON public.subscriptions;
CREATE POLICY "Service role / Admin full access to subscriptions" 
ON public.subscriptions FOR ALL 
TO authenticated 
USING (
  auth.uid() = user_id 
  OR (auth.jwt() ->> 'email') IN ('shamilajw999@gmail.com')
  OR current_user = 'service_role'
);

-- 7. Storage bucket for payment receipts
INSERT INTO storage.buckets (id, name, public) 
VALUES ('payment-receipts', 'payment-receipts', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Receipts Public Read" ON storage.objects;
CREATE POLICY "Receipts Public Read" 
ON storage.objects FOR SELECT 
USING (bucket_id = 'payment-receipts');

DROP POLICY IF EXISTS "Authenticated users upload receipts" ON storage.objects;
CREATE POLICY "Authenticated users upload receipts" 
ON storage.objects FOR INSERT 
TO authenticated 
WITH CHECK (bucket_id = 'payment-receipts');

-- 8. RPC: admin_approve_payment_order
CREATE OR REPLACE FUNCTION public.admin_approve_payment_order(
    p_order_id UUID,
    p_verified_by TEXT DEFAULT 'admin'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_order RECORD;
    v_sub_id UUID := gen_random_uuid();
    v_duration INTERVAL;
    v_expires_at TIMESTAMPTZ;
    v_normalized_plan TEXT;
BEGIN
    SELECT * INTO v_order FROM public.payment_orders WHERE id = p_order_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Payment order not found');
    END IF;

    -- Normalize plan names: max/prime = 1 year, pro = 30 days
    v_normalized_plan := CASE WHEN LOWER(v_order.plan) IN ('max', 'prime') THEN 'max' ELSE 'pro' END;
    v_duration := CASE WHEN v_normalized_plan = 'max' THEN INTERVAL '365 days' ELSE INTERVAL '30 days' END;
    v_expires_at := now() + v_duration;

    -- Update Order
    UPDATE public.payment_orders
    SET status = 'verified',
        verified_at = now(),
        verified_by = p_verified_by,
        subscription_id = v_sub_id
    WHERE id = p_order_id;

    -- Upsert Active Subscription
    INSERT INTO public.subscriptions (id, user_id, plan, status, payment_order_id, started_at, expires_at)
    VALUES (v_sub_id, v_order.user_id, v_normalized_plan, 'active', p_order_id, now(), v_expires_at);

    -- Update Profile tier
    UPDATE public.profiles
    SET subscription_tier = v_normalized_plan
    WHERE id = v_order.user_id;

    RETURN jsonb_build_object(
        'success', true,
        'order_id', p_order_id,
        'user_id', v_order.user_id,
        'plan', v_normalized_plan,
        'status', 'verified',
        'expires_at', v_expires_at
    );
END;
$$;

-- 9. RPC: admin_reject_payment_order
CREATE OR REPLACE FUNCTION public.admin_reject_payment_order(
    p_order_id UUID,
    p_rejection_reason TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.payment_orders
    SET status = 'rejected',
        rejection_reason = p_rejection_reason
    WHERE id = p_order_id;

    RETURN jsonb_build_object('success', true, 'order_id', p_order_id, 'status', 'rejected');
END;
$$;

-- 10. RPC: get_user_entitlements
CREATE OR REPLACE FUNCTION public.get_user_entitlements(p_user_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_sub RECORD;
    v_profile RECORD;
    v_is_active BOOLEAN := false;
    v_tier TEXT := 'free';
BEGIN
    SELECT * INTO v_profile FROM public.profiles WHERE id = p_user_id;
    
    SELECT * INTO v_sub 
    FROM public.subscriptions 
    WHERE user_id = p_user_id 
      AND status = 'active' 
      AND expires_at > now()
    ORDER BY expires_at DESC 
    LIMIT 1;

    IF FOUND THEN
        v_is_active := true;
        v_tier := v_sub.plan;
    ELSIF v_profile.subscription_tier IS NOT NULL AND v_profile.subscription_tier != 'free' THEN
        v_is_active := true;
        v_tier := v_profile.subscription_tier;
    END IF;

    RETURN jsonb_build_object(
        'user_id', p_user_id,
        'has_active_subscription', v_is_active,
        'tier', v_tier,
        'is_pro', (v_is_active AND v_tier IN ('pro', 'max', 'prime')),
        'is_prime', (v_is_active AND v_tier IN ('max', 'prime')),
        'expires_at', v_sub.expires_at
    );
END;
$$;
