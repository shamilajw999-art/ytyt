-- =========================================================================
-- READ-ONLY COMPREHENSIVE DATABASE & RLS DIAGNOSTIC SCRIPT (V2)
-- Copy and paste this directly into your Supabase SQL Editor.
-- This script does NOT modify any data, schema, indexes, or policies.
-- =========================================================================

-- SECTION 1: TABLE STRUCTURE, COLUMNS, TYPES, NULLABILITY & DEFAULTS
SELECT 
    table_schema,
    table_name,
    column_name,
    data_type,
    udt_name,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public' 
  AND table_name IN ('profiles', 'payment_orders', 'subscriptions', 'admin_audit_logs')
ORDER BY table_name, ordinal_position;

-- SECTION 2: PRIMARY KEYS, FOREIGN KEYS, UNIQUE CONSTRAINTS & CHECK CONSTRAINTS
SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_schema AS referenced_table_schema,
    ccu.table_name AS referenced_table_name,
    ccu.column_name AS referenced_column_name,
    cc.check_clause
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name 
    AND tc.table_schema = kcu.table_schema
LEFT JOIN information_schema.constraint_column_usage ccu 
    ON ccu.constraint_name = tc.constraint_name 
    AND ccu.table_schema = tc.table_schema
LEFT JOIN information_schema.check_constraints cc
    ON cc.constraint_name = tc.constraint_name
WHERE tc.table_schema = 'public'
  AND tc.table_name IN ('profiles', 'payment_orders', 'subscriptions', 'admin_audit_logs');

-- SECTION 3: INDEXES
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('profiles', 'payment_orders', 'subscriptions', 'admin_audit_logs');

-- SECTION 4: ROW LEVEL SECURITY (RLS) STATUS & POLICIES
SELECT 
    c.relname AS table_name,
    c.relrowsecurity AS rls_enabled,
    c.relforcerowsecurity AS rls_forced
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname IN ('profiles', 'payment_orders', 'subscriptions', 'admin_audit_logs');

SELECT 
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('profiles', 'payment_orders', 'subscriptions', 'admin_audit_logs');

-- SECTION 5: FUNCTIONS & SECURITY DEFINER PROPERTIES (FIXED CATALOG JOIN)
SELECT 
    p.proname AS function_name,
    pg_get_function_arguments(p.oid) AS arguments,
    t.typname AS return_type,
    p.prosecdef AS is_security_definer,
    p.proconfig AS search_path,
    pg_get_functiondef(p.oid) AS definition
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
JOIN pg_type t ON p.prorettype = t.oid
WHERE n.nspname = 'public'
  AND p.proname IN ('approve_payment_order', 'reject_payment_order', 'get_user_entitlements', 'update_user_profile');

-- SECTION 6: DATA INTEGRITY & ANOMALY DIAGNOSTICS (TYPE-SAFE JOIN CASTS)

-- A. Duplicate non-empty bank_transaction_id values in payment_orders
SELECT 'Duplicate Bank Transaction IDs' AS check_name, bank_transaction_id, COUNT(*) AS count
FROM public.payment_orders
WHERE bank_transaction_id IS NOT NULL AND bank_transaction_id != ''
GROUP BY bank_transaction_id
HAVING COUNT(*) > 1;

-- B. Duplicate active subscriptions for the same user
SELECT 'Duplicate Active Subscriptions' AS check_name, user_id::text, COUNT(*) AS count
FROM public.subscriptions
WHERE status = 'active'
GROUP BY user_id::text
HAVING COUNT(*) > 1;

-- C. Payment orders with missing or invalid user_id (not in auth.users)
SELECT 'Orphan Payment Orders' AS check_name, po.id, po.user_id::text
FROM public.payment_orders po
LEFT JOIN auth.users u ON po.user_id::text = u.id::text
WHERE u.id IS NULL;

-- D. Subscriptions with missing or invalid user_id (not in auth.users)
SELECT 'Orphan Subscriptions' AS check_name, s.id, s.user_id::text
FROM public.subscriptions s
LEFT JOIN auth.users u ON s.user_id::text = u.id::text
WHERE u.id IS NULL;

-- E. Profiles without matching auth.users record
SELECT 'Orphan Profiles' AS check_name, p.id::text, p.email
FROM public.profiles p
LEFT JOIN auth.users u ON p.id::text = u.id::text
WHERE u.id IS NULL;

-- F. Active subscriptions with expired expires_at
SELECT 'Expired Active Subscriptions' AS check_name, id, user_id::text, expires_at
FROM public.subscriptions
WHERE status = 'active' AND expires_at < NOW();
