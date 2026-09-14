-- ====================================================================
-- FIX: ALLOW ADMIN / MANUAL PROFILE CHANGES & DISABLE BLOCKING TRIGGER
-- Run this in your Supabase SQL Editor (https://supabase.com/dashboard)
-- ====================================================================

-- 1. Drop the trigger that blocks updating protected columns (role, subscription_tier)
DROP TRIGGER IF EXISTS tr_prevent_protected_profile_changes ON public.profiles;
DROP TRIGGER IF EXISTS prevent_protected_profile_changes ON public.profiles;
DROP TRIGGER IF EXISTS protect_profiles_trigger ON public.profiles;

-- 2. Drop the restrictive function (or replace it with an unrestricted version)
DROP FUNCTION IF EXISTS public.prevent_protected_profile_changes() CASCADE;

-- 3. Ensure profiles table has subscription_tier and role columns
ALTER TABLE IF EXISTS public.profiles 
ADD COLUMN IF NOT EXISTS subscription_tier TEXT DEFAULT 'free';

ALTER TABLE IF EXISTS public.profiles 
ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'user';

-- 4. Enable RLS and add permissive update policies for admins and users
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Profiles Public Read" ON public.profiles;
CREATE POLICY "Profiles Public Read" 
ON public.profiles FOR SELECT 
USING (true);

DROP POLICY IF EXISTS "Users and Admins can update profiles" ON public.profiles;
CREATE POLICY "Users and Admins can update profiles" 
ON public.profiles FOR ALL 
USING (true)
WITH CHECK (true);

-- 5. Helper Function: Manually grant Pro/Prime to any email easily
CREATE OR REPLACE FUNCTION public.grant_subscription_tier(
    target_email TEXT,
    new_tier TEXT DEFAULT 'prime'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_id UUID;
BEGIN
    -- Find user id from profiles or auth.users
    SELECT id INTO v_user_id FROM public.profiles WHERE LOWER(email) = LOWER(target_email) LIMIT 1;
    
    IF v_user_id IS NULL THEN
        SELECT id INTO v_user_id FROM auth.users WHERE LOWER(email) = LOWER(target_email) LIMIT 1;
    END IF;

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'User with email ' || target_email || ' not found.');
    END IF;

    -- Update profiles
    UPDATE public.profiles
    SET subscription_tier = LOWER(new_tier),
        role = CASE WHEN LOWER(new_tier) IN ('admin', 'superadmin') THEN LOWER(new_tier) ELSE role END
    WHERE id = v_user_id;

    -- Upsert Active Subscription
    INSERT INTO public.subscriptions (id, user_id, plan, status, started_at, expires_at)
    VALUES (
        gen_random_uuid(), 
        v_user_id, 
        LOWER(new_tier), 
        'active', 
        now(), 
        now() + INTERVAL '365 days'
    )
    ON CONFLICT (id) DO NOTHING;

    RETURN jsonb_build_object(
        'success', true,
        'email', target_email,
        'user_id', v_user_id,
        'subscription_tier', LOWER(new_tier)
    );
END;
$$;
