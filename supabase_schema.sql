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

-- ------------------------------------------------------------------------------
-- 6. TELECOM SITES DATABASE TABLE (site_db)
-- ------------------------------------------------------------------------------
-- Stores the authoritative national directory of telecom towers, masts, monopoles,
-- and microcells across Mobiserve, Djezzy, Mobilis, and Ooredoo networks.
CREATE TABLE IF NOT EXISTS public.site_db (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id TEXT UNIQUE NOT NULL,
    site_name TEXT NOT NULL,
    operator TEXT NOT NULL DEFAULT 'Mobiserve',
    latitude DOUBLE PRECISION NOT NULL DEFAULT 36.754800,
    longitude DOUBLE PRECISION NOT NULL DEFAULT 3.056900,
    altitude DOUBLE PRECISION DEFAULT 45.0,
    address TEXT DEFAULT 'Algiers, Algeria',
    region TEXT DEFAULT 'National',
    site_type TEXT DEFAULT 'Rooftop Tower',
    access_status TEXT DEFAULT 'PENDING' CHECK (access_status IN ('ACCORDED', 'REFUSED', 'PENDING')),
    refusal_reason TEXT,
    access_instructions TEXT DEFAULT 'Access badge & ID required at security post',
    contact_person TEXT,
    contact_phone TEXT,
    last_audit_timestamp BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for lightning fast searching and geographic queries
CREATE INDEX IF NOT EXISTS idx_site_db_site_id ON public.site_db (site_id);
CREATE INDEX IF NOT EXISTS idx_site_db_site_name ON public.site_db (site_name);
CREATE INDEX IF NOT EXISTS idx_site_db_operator ON public.site_db (operator);
CREATE INDEX IF NOT EXISTS idx_site_db_access_status ON public.site_db (access_status);

-- Enable RLS and grant read access to anonymous clients
ALTER TABLE public.site_db ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow select site_db" ON public.site_db;
CREATE POLICY "Allow select site_db" ON public.site_db FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert site_db" ON public.site_db;
CREATE POLICY "Allow insert site_db" ON public.site_db FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow update site_db" ON public.site_db;
CREATE POLICY "Allow update site_db" ON public.site_db FOR UPDATE USING (true);

-- Seed Initial Algerian Telecom Sites into site_db
INSERT INTO public.site_db (site_id, site_name, operator, latitude, longitude, altitude, address, region, site_type, access_status, access_instructions, contact_person, contact_phone)
VALUES
    ('A16X1631', 'Q35 LPP Sidi Abdellah', 'Mobiserve', 36.671200, 2.871500, 185.0, 'Quartier 35 LPP, Nouvelle Ville Sidi Abdellah, Alger', 'ALGER (Sidi Abdellah)', 'Rooftop Tower', 'ACCORDED', 'Accès conciergerie LPP Q35. Clé armoire technique avec gardien principal.', 'Mourad Cherif (Gardien LPP Q35)', '+213 550 35 16 31'),
    ('A16X', 'Alger Centre Pasteur Relay', 'Mobiserve', 36.772500, 3.058800, 54.0, 'Boulevard Colonel Amirouche / Pasteur, Alger Centre', 'Algiers Central', 'Rooftop Tower', 'ACCORDED', 'Security badge & ID required at concierge. Key ref: #K-16 Pasteur', 'Karim Benali (NOC Coordinator)', '+213 550 16 16 16'),
    ('MOB-ALG-1044', 'Hydra Telecom Tower', 'Mobiserve', 36.754800, 3.056900, 48.0, 'Rue Djenane El Malik, Hydra, Algiers', 'Algiers Central', 'Rooftop Tower', 'ACCORDED', 'Security badge & ID required at reception gate', 'Farid Touati (Site Manager)', '+213 550 99 88 77'),
    ('DJZ-ALG-0218', 'Bab Ezzouar Cyberpark Mast', 'Djezzy', 36.721400, 3.182500, 32.0, 'Cyberpark Sidi Abdellah / Bab Ezzouar', 'Algiers East', 'Greenfield Mast', 'PENDING', 'Work permit signed by NOC required for climbing', 'Nassim Brahimi', '+213 551 11 22 33'),
    ('MOB-ALG-0512', 'Kouba Plateau Central', 'Mobilis', 36.732800, 3.078900, 115.0, 'Avenue des Frères Abdeslami, Kouba, Algiers', 'Algiers South', 'Monopole', 'ACCORDED', 'Security gate combination code: 4492', 'Karim Cherif', '+213 552 33 44 55'),
    ('OOR-ALG-0891', 'El Biar Relay Station', 'Ooredoo', 36.768500, 3.032100, 160.0, 'Chemin Mackley, El Biar, Algiers', 'Algiers West', 'Rooftop Tower', 'REFUSED', 'Contact building manager M. Zinedine prior to arrival', 'Zinedine Kaci', '+213 553 44 55 66'),
    ('MOB-ALG-1205', 'Bir Mourad Rais Monopole', 'Mobiserve', 36.741000, 3.051000, 78.0, 'Route de Blida, Bir Mourad Rais, Algiers', 'Algiers Central', 'Monopole', 'ACCORDED', 'Security guard M. Amine on duty 24/7', 'Amine Djelloul', '+213 554 55 66 77'),
    ('DJZ-ALG-0734', 'Sidi Yahia Commercial Microcell', 'Djezzy', 36.758200, 3.039500, 95.0, 'Boulevard Sidi Yahia, Hydra, Algiers', 'Algiers Central', 'Indoor Microcell', 'ACCORDED', 'Access via commercial mall service staircase after 08:00', 'Reda Mansouri', '+213 555 66 77 88'),
    ('MOB-ORN-0102', 'Oran Canastel Coastal Mast', 'Mobiserve', 35.731200, -0.569400, 85.0, 'Boulevard des Falaises, Canastel, Oran', 'Oran Littoral', 'Greenfield Mast', 'ACCORDED', 'Access permit stamped by Oran regional directorate', 'Sofiane Belkacem', '+213 556 77 88 99'),
    ('DJZ-CST-0304', 'Constantine Sidi M''Cid Gorge Tower', 'Djezzy', 36.368900, 6.614700, 640.0, 'Route de Sidi M''Cid, Constantine', 'Constantine Est', 'Greenfield Tower', 'ACCORDED', 'Climbing harness & dual lanyard required on steel structure', 'Tarek Boukhatem', '+213 557 88 99 00'),
    ('OOR-ANN-0415', 'Annaba Seybouse Industrial Relay', 'Ooredoo', 36.879400, 7.755100, 15.0, 'Zone Industrielle Seybouse, Annaba', 'Annaba Port', 'Monopole', 'PENDING', 'Factory safety briefing required at security cabin', 'Walid Benali', '+213 558 99 00 11'),
    ('MOB-BLD-0620', 'Blida Chiffa Gorges Monopole', 'Mobilis', 36.467800, 2.768100, 280.0, 'RN1 Gorges de la Chiffa, Blida', 'Blida Sud', 'Monopole', 'ACCORDED', 'Access key with forestry post supervisor', 'Khaled Meziane', '+213 559 00 11 22')
ON CONFLICT (site_id) DO UPDATE
SET site_name = EXCLUDED.site_name,
    operator = EXCLUDED.operator,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    altitude = EXCLUDED.altitude,
    address = EXCLUDED.address,
    region = EXCLUDED.region,
    site_type = EXCLUDED.site_type,
    access_status = EXCLUDED.access_status,
    access_instructions = EXCLUDED.access_instructions,
    contact_person = EXCLUDED.contact_person,
    contact_phone = EXCLUDED.contact_phone,
    updated_at = NOW();

