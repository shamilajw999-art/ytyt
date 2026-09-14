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
