-- ==============================================================================
-- SUPABASE POSTGRESQL SCHEMA FOR TELECOM SITE ACCESS AUDIT (T-SAG)
-- ==============================================================================
-- This schema supports:
-- 1. User accounts, roles (ADMIN, OM, SUPERVISION, TECHNICIAN, DISPATCHER, ACQUISITION)
-- 2. Account activation workflow: users register with email/password and wait for admin approval
-- 3. Site Reports database: stores telecom field site audit reports and telemetry
-- 4. Row Level Security (RLS) policies
-- ==============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------------------------
-- 1. PROFILES TABLE (User Directory & Roles)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT,
    role TEXT NOT NULL DEFAULT 'TECHNICIAN' CHECK (role IN ('ADMIN', 'OM', 'SUPERVISION', 'TECHNICIAN', 'DISPATCHER', 'ACQUISITION')),
    department TEXT DEFAULT 'Telecom Field Operations',
    region TEXT DEFAULT 'National',
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast lookup by email and activation state
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles (email);
CREATE INDEX IF NOT EXISTS idx_profiles_is_active ON public.profiles (is_active);

-- ------------------------------------------------------------------------------
-- 2. SITE REPORTS TABLE (Telecom Site Access & Audit Records)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.site_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id TEXT NOT NULL,
    site_name TEXT,
    operator TEXT,
    status TEXT NOT NULL DEFAULT 'ACCORDED' CHECK (status IN ('ACCORDED', 'REFUSED')),
    refusal_reason TEXT,
    notes TEXT,
    technician_name TEXT,
    technician_phone TEXT,
    technician_email TEXT,
    technician_uid TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    accuracy REAL,
    address TEXT,
    gsm_operator TEXT,
    network_type TEXT,
    cell_id TEXT,
    lac TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for querying reports by site_id and technician
CREATE INDEX IF NOT EXISTS idx_site_reports_site_id ON public.site_reports (site_id);
CREATE INDEX IF NOT EXISTS idx_site_reports_technician_email ON public.site_reports (technician_email);
CREATE INDEX IF NOT EXISTS idx_site_reports_created_at ON public.site_reports (created_at DESC);

-- ------------------------------------------------------------------------------
-- 3. AUTOMATIC PROFILE CREATION TRIGGER (Optional on Supabase Auth Sign-Up)
-- ------------------------------------------------------------------------------
-- When a user registers through Supabase Auth (auth.users), this trigger creates a
-- pending (is_active = false) profile awaiting administrator activation.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    is_root_admin BOOLEAN;
BEGIN
    -- Initial super-admins are activated automatically
    is_root_admin := LOWER(NEW.email) IN ('maamardjemil@gmail.com', 'etb.djemil@gmail.com', 'mdjemil@mobiserveholding.com');

    INSERT INTO public.profiles (id, name, email, phone, role, is_active, created_at, updated_at)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'name', split_part(NEW.email, '@', 1)),
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'phone', ''),
        CASE WHEN is_root_admin THEN 'ADMIN' ELSE COALESCE(NEW.raw_user_meta_data->>'role', 'TECHNICIAN') END,
        CASE WHEN is_root_admin THEN true ELSE false END,
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name,
        updated_at = NOW();

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop trigger if existing and recreate
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ------------------------------------------------------------------------------
-- 4. ROW LEVEL SECURITY (RLS) POLICIES
-- ------------------------------------------------------------------------------
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.site_reports ENABLE ROW LEVEL SECURITY;

-- Profiles: Anyone authenticated or anonymous client can query directory
DROP POLICY IF EXISTS "Allow select profiles" ON public.profiles;
CREATE POLICY "Allow select profiles" ON public.profiles FOR SELECT USING (true);

-- Profiles: Allow insert/upsert for registration and admin role updates
DROP POLICY IF EXISTS "Allow insert profiles" ON public.profiles;
CREATE POLICY "Allow insert profiles" ON public.profiles FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow update profiles" ON public.profiles;
CREATE POLICY "Allow update profiles" ON public.profiles FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Allow delete profiles" ON public.profiles;
CREATE POLICY "Allow delete profiles" ON public.profiles FOR DELETE USING (true);

-- Site Reports: Allow reading all reports
DROP POLICY IF EXISTS "Allow select site_reports" ON public.site_reports;
CREATE POLICY "Allow select site_reports" ON public.site_reports FOR SELECT USING (true);

-- Site Reports: Allow inserting new audit reports
DROP POLICY IF EXISTS "Allow insert site_reports" ON public.site_reports;
CREATE POLICY "Allow insert site_reports" ON public.site_reports FOR INSERT WITH CHECK (true);

-- ------------------------------------------------------------------------------
-- 5. INITIAL DEFAULT USERS SEED (Optional)
-- ------------------------------------------------------------------------------
INSERT INTO public.profiles (id, name, email, phone, role, department, region, is_active)
VALUES
    ('usr_root_admin', 'M. Djemil', 'mdjemil@mobiserveholding.com', '+213 550 12 34 56', 'ADMIN', 'Management / NOC', 'National', true),
    ('usr_root_admin_2', 'Maamar Djemil', 'maamardjemil@gmail.com', '+213 550 00 00 00', 'ADMIN', 'Management / NOC', 'National', true),
    ('usr_root_admin_3', 'ETB Djemil', 'etb.djemil@gmail.com', '+213 550 11 11 11', 'ADMIN', 'Management / NOC', 'National', true)
ON CONFLICT (email) DO NOTHING;
