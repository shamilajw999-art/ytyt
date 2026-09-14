-- ============================================================================
-- MYTHIC CEYLON - SECURE ADMIN DASHBOARD & PAYMENT SYSTEM SCHEMA
-- ============================================================================

-- 1. PROFILES TABLE ENHANCEMENTS (Role System)
ALTER TABLE IF EXISTS public.profiles 
ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin')),
ADD COLUMN IF NOT EXISTS subscription_tier VARCHAR(20) DEFAULT 'free';

-- Helper function to check admin privileges securely
CREATE OR REPLACE FUNCTION public.is_admin(user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = user_id AND role = 'admin'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. PAYMENT ORDERS TABLE
CREATE TABLE IF NOT EXISTS public.payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user_email VARCHAR(255),
    plan VARCHAR(20) NOT NULL CHECK (plan IN ('pro', 'max')),
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    payment_reference VARCHAR(50) UNIQUE NOT NULL,
    bank_transaction_id VARCHAR(100),
    receipt_image_url TEXT,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'under_review', 'approved', 'rejected')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    verified_by UUID REFERENCES auth.users(id),
    rejection_reason TEXT,
    subscription_id UUID
);

-- Ensure columns exist if table was created in an earlier migration
ALTER TABLE IF EXISTS public.payment_orders
ADD COLUMN IF NOT EXISTS user_email VARCHAR(255),
ADD COLUMN IF NOT EXISTS bank_transaction_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS receipt_image_url TEXT,
ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES auth.users(id),
ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
ADD COLUMN IF NOT EXISTS subscription_id UUID;

-- 3. PAYMENT SUBMISSIONS TABLE
CREATE TABLE IF NOT EXISTS public.payment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_order_id UUID NOT NULL REFERENCES public.payment_orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    bank_transaction_id VARCHAR(100) NOT NULL,
    transfer_amount NUMERIC(10,2) NOT NULL,
    transfer_datetime TIMESTAMPTZ DEFAULT NOW(),
    receipt_image_url TEXT,
    submitted_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. SUBSCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plan VARCHAR(20) NOT NULL CHECK (plan IN ('pro', 'max')),
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'expired', 'cancelled')),
    started_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    payment_order_id UUID REFERENCES public.payment_orders(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add foreign key constraint back to payment_orders if missing
ALTER TABLE public.payment_orders
DROP CONSTRAINT IF EXISTS fk_payment_orders_subscription;

ALTER TABLE public.payment_orders
ADD CONSTRAINT fk_payment_orders_subscription
FOREIGN KEY (subscription_id) REFERENCES public.subscriptions(id) ON DELETE SET NULL;

-- 5. ADMIN AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID NOT NULL REFERENCES auth.users(id),
    action VARCHAR(50) NOT NULL,
    payment_order_id UUID REFERENCES public.payment_orders(id),
    target_user_id UUID REFERENCES auth.users(id),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. INDEXES FOR HIGH-PERFORMANCE QUERIES
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);
CREATE INDEX IF NOT EXISTS idx_payment_orders_user_id ON public.payment_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status ON public.payment_orders(status);
CREATE INDEX IF NOT EXISTS idx_payment_orders_reference ON public.payment_orders(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payment_submissions_order_id ON public.payment_submissions(payment_order_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON public.subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON public.subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_audit_logs_admin_id ON public.admin_audit_logs(admin_user_id);

-- 7. ROW LEVEL SECURITY (RLS) POLICIES

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;

-- Clean up existing policy names before recreating
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update non-role profile fields" ON public.profiles;
DROP POLICY IF EXISTS "Admins can update any profile" ON public.profiles;

DROP POLICY IF EXISTS "Users view own payment orders" ON public.payment_orders;
DROP POLICY IF EXISTS "Users insert own payment orders" ON public.payment_orders;
DROP POLICY IF EXISTS "Admins update payment orders" ON public.payment_orders;

DROP POLICY IF EXISTS "Users view own submissions" ON public.payment_submissions;
DROP POLICY IF EXISTS "Users insert own submissions" ON public.payment_submissions;

DROP POLICY IF EXISTS "Users view own subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Admins manage subscriptions" ON public.subscriptions;

DROP POLICY IF EXISTS "Admins view audit logs" ON public.admin_audit_logs;
DROP POLICY IF EXISTS "Admins insert audit logs" ON public.admin_audit_logs;

-- Profiles Policies
CREATE POLICY "Users can read own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id OR public.is_admin(auth.uid()));

CREATE POLICY "Users can update non-role profile fields" ON public.profiles
    FOR UPDATE USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id AND role = (SELECT role FROM public.profiles WHERE id = auth.uid()));

CREATE POLICY "Admins can update any profile" ON public.profiles
    FOR UPDATE USING (public.is_admin(auth.uid()));

-- Payment Orders Policies
CREATE POLICY "Users view own payment orders" ON public.payment_orders
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin(auth.uid()));

CREATE POLICY "Users insert own payment orders" ON public.payment_orders
    FOR INSERT WITH CHECK (auth.uid() = user_id AND status = 'pending');

CREATE POLICY "Admins update payment orders" ON public.payment_orders
    FOR UPDATE USING (public.is_admin(auth.uid()));

-- Payment Submissions Policies
CREATE POLICY "Users view own submissions" ON public.payment_submissions
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin(auth.uid()));

CREATE POLICY "Users insert own submissions" ON public.payment_submissions
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Subscriptions Policies
CREATE POLICY "Users view own subscriptions" ON public.subscriptions
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin(auth.uid()));

CREATE POLICY "Admins manage subscriptions" ON public.subscriptions
    FOR ALL USING (public.is_admin(auth.uid()));

-- Audit Logs Policies
CREATE POLICY "Admins view audit logs" ON public.admin_audit_logs
    FOR SELECT USING (public.is_admin(auth.uid()));

CREATE POLICY "Admins insert audit logs" ON public.admin_audit_logs
    FOR INSERT WITH CHECK (public.is_admin(auth.uid()));

-- ============================================================================
-- 8. SECURE SERVER-SIDE STORED PROCEDURES FOR APPROVAL / REJECTION
-- ============================================================================

-- FUNCTION: APPROVE PAYMENT ORDER
CREATE OR REPLACE FUNCTION public.approve_payment_order(
    p_order_id UUID,
    p_admin_id UUID,
    p_duration_days INT DEFAULT 365
)
RETURNS JSONB AS $$
DECLARE
    v_order public.payment_orders%ROWTYPE;
    v_sub_id UUID;
    v_exp_date TIMESTAMPTZ;
BEGIN
    -- Check Admin Privilege
    IF NOT public.is_admin(p_admin_id) THEN
        RAISE EXCEPTION 'Unauthorized: Only admin accounts can approve payment orders.';
    END IF;

    -- Fetch payment order
    SELECT * INTO v_order FROM public.payment_orders WHERE id = p_order_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Payment order % not found.', p_order_id;
    END IF;

    IF v_order.status = 'approved' THEN
        RAISE EXCEPTION 'Payment order % is already approved.', p_order_id;
    END IF;

    -- Calculate expiration date
    v_exp_date := NOW() + (p_duration_days || ' days')::INTERVAL;

    -- 1. Create Subscription
    INSERT INTO public.subscriptions (
        user_id, plan, status, started_at, expires_at, payment_order_id
    ) VALUES (
        v_order.user_id, v_order.plan, 'active', NOW(), v_exp_date, p_order_id
    ) RETURNING id INTO v_sub_id;

    -- 2. Update Payment Order Status
    UPDATE public.payment_orders
    SET status = 'approved',
        verified_at = NOW(),
        verified_by = p_admin_id,
        subscription_id = v_sub_id
    WHERE id = p_order_id;

    -- 3. Update User Profile Subscription Tier
    UPDATE public.profiles
    SET subscription_tier = v_order.plan
    WHERE id = v_order.user_id;

    -- 4. Create Audit Log Entry
    INSERT INTO public.admin_audit_logs (
        admin_user_id, action, payment_order_id, target_user_id, metadata
    ) VALUES (
        p_admin_id, 'APPROVE_PAYMENT', p_order_id, v_order.user_id,
        jsonb_build_object(
            'plan', v_order.plan,
            'amount', v_order.amount,
            'reference', v_order.payment_reference,
            'subscription_id', v_sub_id
        )
    );

    RETURN jsonb_build_object(
        'success', true,
        'subscription_id', v_sub_id,
        'message', 'Payment order successfully approved.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- FUNCTION: REJECT PAYMENT ORDER
CREATE OR REPLACE FUNCTION public.reject_payment_order(
    p_order_id UUID,
    p_admin_id UUID,
    p_reason TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_order public.payment_orders%ROWTYPE;
BEGIN
    -- Check Admin Privilege
    IF NOT public.is_admin(p_admin_id) THEN
        RAISE EXCEPTION 'Unauthorized: Only admin accounts can reject payment orders.';
    END IF;

    -- Fetch payment order
    SELECT * INTO v_order FROM public.payment_orders WHERE id = p_order_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Payment order % not found.', p_order_id;
    END IF;

    -- 1. Update Payment Order Status
    UPDATE public.payment_orders
    SET status = 'rejected',
        verified_at = NOW(),
        verified_by = p_admin_id,
        rejection_reason = p_reason
    WHERE id = p_order_id;

    -- 2. Create Audit Log Entry
    INSERT INTO public.admin_audit_logs (
        admin_user_id, action, payment_order_id, target_user_id, metadata
    ) VALUES (
        p_admin_id, 'REJECT_PAYMENT', p_order_id, v_order.user_id,
        jsonb_build_object(
            'reason', p_reason,
            'reference', v_order.payment_reference,
            'amount', v_order.amount
        )
    );

    RETURN jsonb_build_object(
        'success', true,
        'message', 'Payment order rejected.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
-- FUNCTION: GET USER ENTITLEMENTS
CREATE OR REPLACE FUNCTION public.get_user_entitlements()
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_sub public.subscriptions%ROWTYPE;
    v_plan VARCHAR(20) := 'free';
    v_status VARCHAR(20) := 'none';
    v_expires_at TIMESTAMPTZ := NULL;
    v_entitlements JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    -- Get the most recent active subscription for this user
    SELECT * INTO v_sub 
    FROM public.subscriptions 
    WHERE user_id = v_user_id 
      AND status = 'active'
      AND expires_at > NOW()
    ORDER BY expires_at DESC
    LIMIT 1;

    IF FOUND THEN
        v_plan := v_sub.plan;
        v_status := v_sub.status;
        v_expires_at := v_sub.expires_at;
    END IF;

    -- Build entitlements object based on the plan
    IF v_plan = 'max' THEN
        v_entitlements := jsonb_build_object(
            'ads_free', true,
            'unlimited_lumo', true,
            'high_resolution_maps', true,
            'priority_support', true,
            'ar_3d_scan', true,
            'beta_features', true,
            'digital_collection_badges', true,
            'custom_profile_background', true,
            'animated_profile_background', true,
            'advanced_profile_customization', true,
            'exclusive_passport_features', true
        );
    ELSIF v_plan = 'pro' THEN
        v_entitlements := jsonb_build_object(
            'ads_free', true,
            'unlimited_lumo', true,
            'high_resolution_maps', true,
            'priority_support', true,
            'ar_3d_scan', false,
            'beta_features', false,
            'digital_collection_badges', false,
            'custom_profile_background', false,
            'animated_profile_background', false,
            'advanced_profile_customization', false,
            'exclusive_passport_features', false
        );
    ELSE
        -- Free plan
        v_entitlements := jsonb_build_object(
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
        );
    END IF;

    RETURN jsonb_build_object(
        'plan', v_plan,
        'status', v_status,
        'expires_at', v_expires_at,
        'entitlements', v_entitlements
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
-- FUNCTION: CAN USE LUMO
CREATE OR REPLACE FUNCTION public.can_use_lumo()
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_sub public.subscriptions%ROWTYPE;
    v_plan VARCHAR(20) := 'free';
    v_msg_count INT;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    -- Get the most recent active subscription for this user
    SELECT * INTO v_sub 
    FROM public.subscriptions 
    WHERE user_id = v_user_id 
      AND status = 'active'
      AND expires_at > NOW()
    ORDER BY expires_at DESC
    LIMIT 1;

    IF FOUND THEN
        v_plan := v_sub.plan;
    END IF;

    IF v_plan IN ('pro', 'max') THEN
        RETURN jsonb_build_object('allowed', true, 'reason', 'unlimited_lumo');
    END IF;

    -- For free users, check if they exceeded the daily limit (e.g. 3 messages)
    SELECT COUNT(*) INTO v_msg_count
    FROM public.mythic_messages -- wait, do we have mythic_messages in supabase? Yes, SupabaseMythicMessage
    WHERE user_id = v_user_id
      AND role = 'user'
      AND created_at > NOW() - INTERVAL '1 day';
      
    IF v_msg_count >= 3 THEN
        RETURN jsonb_build_object('allowed', false, 'reason', 'daily_limit_reached');
    END IF;

    RETURN jsonb_build_object('allowed', true, 'reason', 'free_tier_available');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
-- 9. USER DATA SYNCHRONIZATION & PROFILE REPAIR RPC FUNCTIONS
-- ============================================================================

-- Function: Automatic Profile Creation Trigger on auth.users (Ensures every auth user has a profile)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (
        id, username, email, full_name, avatar_url, role, subscription_tier, level, xp, streak, scans_count, badges_count
    ) VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
        COALESCE(NEW.raw_user_meta_data->>'avatar_url', 'https://api.dicebear.com/7.x/avataaars/svg?seed=' || NEW.id),
        'user',
        'free',
        1,
        0,
        1,
        0,
        0
    )
    ON CONFLICT (id) DO UPDATE SET
        email = EXCLUDED.email,
        username = COALESCE(public.profiles.username, EXCLUDED.username),
        full_name = COALESCE(public.profiles.full_name, EXCLUDED.full_name),
        avatar_url = COALESCE(public.profiles.avatar_url, EXCLUDED.avatar_url);

    INSERT INTO public.user_preferences (user_id, dark_mode, notifications_enabled, language)
    VALUES (NEW.id, true, true, 'en')
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- Function: Repair and backfill missing profiles for existing auth users
CREATE OR REPLACE FUNCTION public.repair_missing_profiles()
RETURNS INT AS $$
DECLARE
    v_count INT := 0;
BEGIN
    INSERT INTO public.profiles (
        id, username, email, full_name, avatar_url, role, subscription_tier, level, xp, streak, scans_count, badges_count
    )
    SELECT
        u.id,
        COALESCE(u.raw_user_meta_data->>'username', split_part(u.email, '@', 1)),
        u.email,
        COALESCE(u.raw_user_meta_data->>'full_name', split_part(u.email, '@', 1)),
        COALESCE(u.raw_user_meta_data->>'avatar_url', 'https://api.dicebear.com/7.x/avataaars/svg?seed=' || u.id),
        'user',
        'free',
        1,
        0,
        1,
        0,
        0
    FROM auth.users u
    LEFT JOIN public.profiles p ON p.id = u.id
    WHERE p.id IS NULL
    ON CONFLICT (id) DO NOTHING;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function: Atomic Award XP RPC (Server-side level and XP calculation)
CREATE OR REPLACE FUNCTION public.award_xp(
    p_amount INT,
    p_user_id UUID DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_target_id UUID;
    v_current_xp INT;
    v_new_xp INT;
    v_new_level INT;
BEGIN
    v_target_id := COALESCE(p_user_id, auth.uid());
    IF v_target_id IS NULL THEN
        RAISE EXCEPTION 'User not authenticated';
    END IF;

    -- Ensure profile exists
    INSERT INTO public.profiles (
        id, username, email, full_name, avatar_url, role, subscription_tier, level, xp, streak, scans_count, badges_count
    )
    SELECT
        u.id,
        COALESCE(u.raw_user_meta_data->>'username', split_part(u.email, '@', 1)),
        u.email,
        COALESCE(u.raw_user_meta_data->>'full_name', split_part(u.email, '@', 1)),
        'https://api.dicebear.com/7.x/avataaars/svg?seed=' || u.id,
        'user',
        'free',
        1,
        0,
        1,
        0,
        0
    FROM auth.users u
    WHERE u.id = v_target_id
    ON CONFLICT (id) DO NOTHING;

    -- Fetch current XP with row-lock
    SELECT xp INTO v_current_xp
    FROM public.profiles
    WHERE id = v_target_id
    FOR UPDATE;

    IF v_current_xp IS NULL THEN
        v_current_xp := 0;
    END IF;

    v_new_xp := GREATEST(0, v_current_xp + p_amount);
    v_new_level := (v_new_xp / 600) + 1;

    UPDATE public.profiles
    SET xp = v_new_xp,
        level = v_new_level,
        updated_at = NOW()
    WHERE id = v_target_id;

    RETURN jsonb_build_object(
        'success', true,
        'user_id', v_target_id,
        'xp', v_new_xp,
        'level', v_new_level,
        'amount_added', p_amount
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function: Atomic Increment Scans Count RPC
CREATE OR REPLACE FUNCTION public.increment_scan_count(
    p_user_id UUID DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_target_id UUID;
    v_new_count INT;
BEGIN
    v_target_id := COALESCE(p_user_id, auth.uid());
    IF v_target_id IS NULL THEN
        RAISE EXCEPTION 'User not authenticated';
    END IF;

    UPDATE public.profiles
    SET scans_count = scans_count + 1,
        updated_at = NOW()
    WHERE id = v_target_id
    RETURNING scans_count INTO v_new_count;

    RETURN jsonb_build_object(
        'success', true,
        'user_id', v_target_id,
        'scans_count', v_new_count
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function: Atomic Increment Badges Count RPC
CREATE OR REPLACE FUNCTION public.increment_badge_count(
    p_user_id UUID DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_target_id UUID;
    v_new_count INT;
BEGIN
    v_target_id := COALESCE(p_user_id, auth.uid());
    IF v_target_id IS NULL THEN
        RAISE EXCEPTION 'User not authenticated';
    END IF;

    UPDATE public.profiles
    SET badges_count = (
        SELECT COUNT(*) FROM public.badges WHERE user_id = v_target_id
    ),
    updated_at = NOW()
    WHERE id = v_target_id
    RETURNING badges_count INTO v_new_count;

    RETURN jsonb_build_object(
        'success', true,
        'user_id', v_target_id,
        'badges_count', v_new_count
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

