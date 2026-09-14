1. **Create a New Project**: Go to [Supabase](https://supabase.com/) and create a new project.
2. **Update Secrets**: Once created, go to Project Settings -> API. Copy your new `Project URL` and `anon public` key. In AI Studio, open the **Secrets** panel and update your `SUPABASE_URL` and `SUPABASE_KEY`.
3. **Run Schema**: Go to the SQL Editor in your new Supabase project and run the following script to create the necessary tables:

```sql
-- Profiles table
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users NOT NULL PRIMARY KEY,
    full_name TEXT,
    avatar_url TEXT,
    xp INTEGER DEFAULT 0,
    streak INTEGER DEFAULT 0,
    scans_count INTEGER DEFAULT 0,
    badges_count INTEGER DEFAULT 0,
    subscription_tier TEXT DEFAULT 'free',
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Subscriptions table
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users NOT NULL,
    tier TEXT NOT NULL,
    status TEXT DEFAULT 'pending', -- 'pending', 'active', 'expired'
    payment_method TEXT DEFAULT 'bank_transfer',
    transaction_id TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- RLS Policies (Enable as needed)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
```

4. **Setup Automated Emails (Edge Functions)**:
The app is configured to call a Supabase Edge Function named `send-payment-email`. To make this functional:
1. Install the Supabase CLI locally.
2. Run `supabase functions new send-payment-email`.
3. Use a service like **Resend** or **SendGrid** in the function to send the email.
4. Deployment command: `supabase functions deploy send-payment-email --no-verify-jwt`.

Example logic for `index.ts` in your function:
```typescript
Deno.serve(async (req) => {
  const { email, tier, amount, transaction_id } = await req.json()
  // Use your preferred email provider API here to send the details
  console.log(`Sending bank details for ${tier} to ${email}`)
  return new Response(JSON.stringify({ success: true }), { headers: { "Content-Type": "application/json" } })
})
```

5. **Setup Subscription and Profile Fields**:
The app requires additional fields in the `profiles` table to manage subscriptions and customizations.
1. Go to **Database** > **Tables** in your Supabase dashboard.
2. Select your `profiles` table.
3. Add the following columns:
   - `subscription_tier`: `text`, default: `'free'`
   - `profile_background_url`: `text`, nullable: `true`
   - `is_animated_background`: `boolean`, default: `false`
   - `bio`: `text`, nullable: `true`
   - `interests`: `text[]`, nullable: `true`
   - `featured_badge_id`: `text`, nullable: `true`
4. Click **Save**.
