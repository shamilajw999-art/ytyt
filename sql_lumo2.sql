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

    -- For free users, check if they exceeded the daily limit (3 messages)
    SELECT COUNT(*) INTO v_msg_count
    FROM public.lumo_conversations
    WHERE user_id = v_user_id
      AND role = 'user'
      AND timestamp > NOW() - INTERVAL '1 day';
      
    IF v_msg_count >= 3 THEN
        RETURN jsonb_build_object('allowed', false, 'reason', 'daily_limit_reached');
    END IF;

    RETURN jsonb_build_object('allowed', true, 'reason', 'free_tier_available');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
