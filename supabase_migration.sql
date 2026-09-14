-- 1. Create approve_payment_order RPC
CREATE OR REPLACE FUNCTION public.approve_payment_order(p_order_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_order public.payment_orders%ROWTYPE;
    v_admin_id UUID;
    v_is_admin BOOLEAN;
    v_expires_at TIMESTAMPTZ;
    v_now TIMESTAMPTZ := NOW();
    v_subscription_id UUID;
BEGIN
    v_admin_id := auth.uid();
    
    -- Check if user is admin
    SELECT (role = 'admin') INTO v_is_admin FROM public.profiles WHERE id = v_admin_id;
    IF NOT v_is_admin THEN
        RAISE EXCEPTION 'Not authorized';
    END IF;

    -- Get payment order
    SELECT * INTO v_order FROM public.payment_orders WHERE id = p_order_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found';
    END IF;

    IF v_order.status = 'approved' THEN
        RAISE EXCEPTION 'Already approved';
    END IF;

    -- Approve
    v_expires_at := v_now + INTERVAL '1 year';

    -- Insert/Update subscription
    INSERT INTO public.subscriptions (user_id, plan, status, payment_order_id, started_at, expires_at)
    VALUES (v_order.user_id, v_order.plan, 'active', v_order.id, v_now, v_expires_at)
    RETURNING id INTO v_subscription_id;

    -- Update order
    UPDATE public.payment_orders 
    SET status = 'approved', 
        verified_at = v_now, 
        verified_by = v_admin_id, 
        subscription_id = v_subscription_id 
    WHERE id = p_order_id;

    -- Update profile tier
    UPDATE public.profiles SET subscription_tier = v_order.plan WHERE id = v_order.user_id;

    -- Audit log
    INSERT INTO public.admin_audit_logs (admin_user_id, action, payment_order_id, target_user_id, metadata)
    VALUES (v_admin_id::text, 'PAYMENT_APPROVED', v_order.id, v_order.user_id, jsonb_build_object('plan', v_order.plan));

    RETURN TRUE;
END;
$$;

-- 2. Create reject_payment_order RPC
CREATE OR REPLACE FUNCTION public.reject_payment_order(p_order_id UUID, p_reason TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_order public.payment_orders%ROWTYPE;
    v_admin_id UUID;
    v_is_admin BOOLEAN;
    v_now TIMESTAMPTZ := NOW();
BEGIN
    v_admin_id := auth.uid();
    
    -- Check if user is admin
    SELECT (role = 'admin') INTO v_is_admin FROM public.profiles WHERE id = v_admin_id;
    IF NOT v_is_admin THEN
        RAISE EXCEPTION 'Not authorized';
    END IF;

    -- Get payment order
    SELECT * INTO v_order FROM public.payment_orders WHERE id = p_order_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found';
    END IF;

    IF v_order.status = 'approved' OR v_order.status = 'rejected' THEN
        RAISE EXCEPTION 'Already processed';
    END IF;

    -- Update order
    UPDATE public.payment_orders 
    SET status = 'rejected', 
        rejection_reason = p_reason,
        verified_at = v_now, 
        verified_by = v_admin_id 
    WHERE id = p_order_id;

    -- Audit log
    INSERT INTO public.admin_audit_logs (admin_user_id, action, payment_order_id, target_user_id, metadata)
    VALUES (v_admin_id::text, 'PAYMENT_REJECTED', v_order.id, v_order.user_id, jsonb_build_object('reason', p_reason));

    RETURN TRUE;
END;
$$;

-- 3. Create get_user_entitlements RPC
CREATE OR REPLACE FUNCTION public.get_user_entitlements()
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_uid UUID;
    v_sub public.subscriptions%ROWTYPE;
    v_res JSON;
    v_is_max BOOLEAN := false;
BEGIN
    v_uid := auth.uid();
    
    SELECT * INTO v_sub FROM public.subscriptions 
    WHERE user_id = v_uid AND status = 'active' AND expires_at > NOW()
    ORDER BY created_at DESC LIMIT 1;

    IF FOUND THEN
        v_is_max := (v_sub.plan = 'max');
        v_res := json_build_object(
            'plan', v_sub.plan,
            'status', v_sub.status,
            'expires_at', v_sub.expires_at,
            'entitlements', json_build_object(
                'ads_free', true,
                'unlimited_lumo', true,
                'high_resolution_maps', true,
                'priority_support', true,
                'ar_3d_scan', v_is_max,
                'beta_features', v_is_max,
                'digital_collection_badges', v_is_max,
                'custom_profile_background', v_is_max,
                'animated_profile_background', v_is_max,
                'advanced_profile_customization', v_is_max,
                'exclusive_passport_features', v_is_max
            )
        );
    ELSE
        v_res := json_build_object(
            'plan', 'free',
            'status', 'expired',
            'expires_at', null,
            'entitlements', json_build_object(
                'ads_free', false,
                'unlimited_lumo', false,
                'high_resolution_maps', false,
                'priority_support', false,
                'ar_3d_scan', false,
                'beta_features', false,
                'digital_collection_badges', false,
                'custom_profile_background', false,
                'animated_profile_background', false,
                'advanced_profile_customization', false,
                'exclusive_passport_features', false
            )
        );
    END IF;

    RETURN v_res;
END;
$$;

-- 4. Enable RLS and setup policies
ALTER TABLE public.payment_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;

-- payment_orders policies
CREATE POLICY "Users can insert their own payment orders" ON public.payment_orders
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view their own payment orders" ON public.payment_orders
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Admins can view all payment orders" ON public.payment_orders
    FOR SELECT USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

-- subscriptions policies
CREATE POLICY "Users can view their own subscriptions" ON public.subscriptions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Admins can view all subscriptions" ON public.subscriptions
    FOR SELECT USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

-- profiles policies
CREATE POLICY "Users can view their own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Admins can view all profiles" ON public.profiles
    FOR SELECT USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

-- admin_audit_logs policies
CREATE POLICY "Admins can view audit logs" ON public.admin_audit_logs
    FOR SELECT USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

