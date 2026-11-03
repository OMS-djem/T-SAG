import { TelecomSite } from '../types';

// Retrieve Supabase credentials with fallback to custom stored credentials
const getEnvVar = (key: string): string => {
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    const metaVal = (import.meta.env as Record<string, string | undefined>)[key] || 
                    (import.meta.env as Record<string, string | undefined>)[`VITE_${key}`];
    if (metaVal) return String(metaVal).trim();
  }
  if (typeof process !== 'undefined' && process.env) {
    const procVal = process.env[key] || process.env[`VITE_${key}`];
    if (procVal) return String(procVal).trim();
  }
  return '';
};

// Check if user set custom credentials in localStorage
const getCustomCredential = (key: string): string => {
  try {
    const val = localStorage.getItem(`tsag_${key}`);
    if (val && val.trim()) return val.trim();
  } catch {
    // Ignore in SSR
  }
  return '';
};

export const getSupabaseUrl = (): string => {
  return (
    getCustomCredential('supabase_url') ||
    getEnvVar('SUPABASE_URL') ||
    'https://idhcctqxfgyrkripecmn.supabase.co'
  );
};

export const getSupabaseAnonKey = (): string => {
  return (
    getCustomCredential('supabase_anon_key') ||
    getEnvVar('SUPABASE_ANON_KEY') ||
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlkaGNjdHF4Zmd5cmtyaXBlY21uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI0Mzg4MTIsImV4cCI6MjEwODAxNDgxMn0.Z3c6uKeyDLGfpmYMNJmK0KOWDiW4EHKFaGF-ETDbikg'
  );
};

export const setCustomSupabaseCredentials = (url: string, key: string): void => {
  try {
    if (url.trim()) localStorage.setItem('tsag_supabase_url', url.trim());
    else localStorage.removeItem('tsag_supabase_url');

    if (key.trim()) localStorage.setItem('tsag_supabase_anon_key', key.trim());
    else localStorage.removeItem('tsag_supabase_anon_key');
  } catch {
    // Ignore in SSR
  }
};

export const SUPABASE_URL = getSupabaseUrl();
export const SUPABASE_ANON_KEY = getSupabaseAnonKey();

export const isSupabaseConfigured = (): boolean => {
  const url = getSupabaseUrl();
  const key = getSupabaseAnonKey();
  return Boolean(
    url && 
    url.startsWith('http') && 
    key && 
    key.length > 20 &&
    !key.includes('dummy_anon_key')
  );
};

/**
 * Universal flexible mapper from any database row (PostgreSQL / Supabase site_db table)
 * into the standardized TelecomSite interface. Handles camelCase, snake_case, and French column names.
 */
export function mapRowToTelecomSite(row: Record<string, unknown>, index: number): TelecomSite {
  // Build a lookup map of normalized keys (lowercase, stripped of spaces, hyphens, underscores, accents)
  const normMap: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(row)) {
    normMap[k] = v;
    normMap[k.toLowerCase()] = v;
    const stripped = k.toLowerCase().replace(/[\s_\-]/g, '').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    normMap[stripped] = v;
  }

  const getStr = (keys: string[]): string => {
    for (const k of keys) {
      const stripped = k.toLowerCase().replace(/[\s_\-]/g, '').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      const val = normMap[k] ?? normMap[k.toLowerCase()] ?? normMap[stripped];
      if (val !== undefined && val !== null && String(val).trim() !== '') {
        return String(val).trim();
      }
    }
    return '';
  };

  const getNum = (keys: string[], defaultVal: number): number => {
    for (const k of keys) {
      const stripped = k.toLowerCase().replace(/[\s_\-]/g, '').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      const val = normMap[k] ?? normMap[k.toLowerCase()] ?? normMap[stripped];
      if (val !== undefined && val !== null) {
        const parsed = parseFloat(String(val));
        if (!isNaN(parsed)) return parsed;
      }
    }
    return defaultVal;
  };

  // Support exact columns from Supabase: "Site code", "Nom du site", "Site Owner", "ID", "Région"
  const siteId = getStr([
    'Site code', 'site code', 'Site Code', 'site_code', 'code_site', 
    'site_id', 'siteId', 'code', 'site', 'id'
  ]) || `SITE-${index + 1}`;

  const siteName = getStr([
    'Nom du site', 'nom du site', 'Nom Du Site', 'nom_du_site', 'nom_site', 
    'site_name', 'siteName', 'designation', 'name', 'nom'
  ]) || siteId;

  let rawOperator = getStr([
    'Site Owner', 'site owner', 'Site owner', 'site_owner', 'operator', 
    'operateur', 'carrier', 'client', 'network'
  ]) || 'Mobiserve';

  // Expand operator abbreviations
  let operator = rawOperator;
  if (/^mbs$/i.test(rawOperator) || /mobiserve/i.test(rawOperator)) operator = 'Mobiserve';
  else if (/^djz$/i.test(rawOperator) || /djezzy/i.test(rawOperator) || /ota/i.test(rawOperator)) operator = 'Djezzy';
  else if (/^mob$/i.test(rawOperator) || /^atm$/i.test(rawOperator) || /mobilis/i.test(rawOperator)) operator = 'Mobilis';
  else if (/^oor$/i.test(rawOperator) || /^wta$/i.test(rawOperator) || /ooredoo/i.test(rawOperator) || /nedjma/i.test(rawOperator)) operator = 'Ooredoo';

  const latitude = getNum(['latitude', 'lat', 'lat_dd', 'coord_lat', 'coord_y', 'y'], 36.671200);
  const longitude = getNum(['longitude', 'lng', 'lon', 'long', 'coord_lng', 'coord_x', 'x'], 2.871500);
  const altitude = getNum(['altitude', 'alt', 'hauteur', 'elevation', 'h'], 185.0);

  const region = getStr(['Région', 'région', 'Region', 'region', 'wilaya', 'zone', 'area', 'division']) || 'National';
  const address = getStr(['address', 'adresse', 'location', 'commune', 'wilaya', 'city']) || `${siteName}, ${region}`;

  const rawType = getStr(['site_type', 'siteType', 'typologie', 'type_site', 'type']);
  let siteType: TelecomSite['siteType'] = 'Rooftop Tower';
  if (/monopole/i.test(rawType)) siteType = 'Monopole';
  else if (/microcell|indoor/i.test(rawType)) siteType = 'Indoor Microcell';
  else if (/mast/i.test(rawType)) siteType = 'Greenfield Mast';
  else if (/greenfield/i.test(rawType)) siteType = 'Greenfield Tower';

  const rawStatus = getStr(['access_status', 'accessStatus', 'status', 'statut']).toUpperCase();
  let accessStatus: TelecomSite['accessStatus'] = 'ACCORDED';
  if (rawStatus.includes('REFUS') || rawStatus.includes('DENI') || rawStatus === 'NO' || rawStatus === 'BLOQUE') {
    accessStatus = 'REFUSED';
  } else if (rawStatus.includes('PEND') || rawStatus.includes('ATTENT')) {
    accessStatus = 'PENDING';
  }

  const refusalReason = getStr(['refusal_reason', 'refusalReason', 'motif_refus', 'reason']) || undefined;
  const accessInstructions = getStr(['access_instructions', 'accessInstructions', 'instructions', 'consignes', 'remarques']) || 
    'Access badge & ID required at security post';
  const contactPerson = getStr(['contact_person', 'contactPerson', 'gardien', 'responsable', 'contact', 'site_manager']);
  const contactPhone = getStr(['contact_phone', 'contactPhone', 'telephone', 'phone', 'tel']);

  const rowId = getStr(['ID', 'id']) || `site_${siteId}_${index}`;

  return {
    id: String(rowId),
    siteId,
    siteName,
    operator,
    latitude,
    longitude,
    altitude,
    address,
    region,
    siteType,
    accessStatus,
    refusalReason,
    accessInstructions,
    contactPerson,
    contactPhone,
    lastAuditTimestamp: row.last_audit_timestamp ? Number(row.last_audit_timestamp) : undefined
  };
}

export const SupabaseSiteService = {
  /**
   * Fetch telecom sites from the `site_db` table in Supabase via PostgREST.
   */
  async fetchSitesFromSiteDb(): Promise<{ sites: TelecomSite[]; source: 'supabase' | 'fallback'; error?: string; rawCount?: number }> {
    const url = getSupabaseUrl();
    const key = getSupabaseAnonKey();

    if (!url || !key) {
      return { 
        sites: [], 
        source: 'fallback', 
        error: 'SUPABASE_URL or SUPABASE_ANON_KEY missing in environment' 
      };
    }

    try {
      const endpoint = `${url.replace(/\/$/, '')}/rest/v1/site_db?select=*&limit=500`;
      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'apikey': key,
          'Authorization': `Bearer ${key}`,
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        return {
          sites: [],
          source: 'fallback',
          error: `HTTP ${response.status}: ${errorText || response.statusText}`
        };
      }

      const rows = await response.json();
      if (Array.isArray(rows) && rows.length > 0) {
        const mapped = rows.map((row, idx) => mapRowToTelecomSite(row, idx));
        return { sites: mapped, source: 'supabase', rawCount: rows.length };
      }

      return {
        sites: [],
        source: 'fallback',
        error: 'site_db table returned 0 records'
      };
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      return {
        sites: [],
        source: 'fallback',
        error: `Supabase connection error: ${msg}`
      };
    }
  },

  /**
   * Search sites directly in the Supabase `site_db` table using PostgREST ilike filters.
   */
  async searchSitesInDb(query: string): Promise<{ sites: TelecomSite[]; error?: string; source: 'supabase' | 'cache' }> {
    const cleanQuery = query.trim();
    if (!cleanQuery) return { sites: [], source: 'supabase' };

    const url = getSupabaseUrl();
    const key = getSupabaseAnonKey();

    if (!url || !key) {
      return { sites: [], error: 'SUPABASE_URL or SUPABASE_ANON_KEY missing in environment', source: 'cache' };
    }

    try {
      // 1. First attempt: PostgREST OR filter with ilike on common columns
      const encoded = encodeURIComponent(`*${cleanQuery}*`);
      const searchEndpoint = `${url.replace(/\/$/, '')}/rest/v1/site_db?select=*&or=(site_id.ilike.${encoded},site_name.ilike.${encoded},operator.ilike.${encoded},address.ilike.${encoded})&limit=25`;

      const response = await fetch(searchEndpoint, {
        method: 'GET',
        headers: {
          'apikey': key,
          'Authorization': `Bearer ${key}`,
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });

      if (response.ok) {
        const rows = await response.json();
        if (Array.isArray(rows) && rows.length > 0) {
          return { sites: rows.map((r, i) => mapRowToTelecomSite(r, i)), source: 'supabase' };
        }
      }

      // 2. Fallback: Query all records and filter in-memory (in case column names in site_db differ)
      const allResult = await this.fetchSitesFromSiteDb();
      if (allResult.sites.length > 0) {
        const q = cleanQuery.toLowerCase();
        const filtered = allResult.sites.filter(site => 
          site.siteId.toLowerCase().includes(q) ||
          site.siteName.toLowerCase().includes(q) ||
          site.operator.toLowerCase().includes(q) ||
          site.address.toLowerCase().includes(q) ||
          site.region.toLowerCase().includes(q)
        );
        return { sites: filtered, source: allResult.source === 'supabase' ? 'supabase' : 'cache' };
      }

      return { sites: [], error: allResult.error, source: 'cache' };
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      return { sites: [], error: msg, source: 'cache' };
    }
  },

  /**
   * Fetch a single site from `site_db` by its site_id code.
   */
  async getSiteByIdFromDb(siteId: string): Promise<TelecomSite | null> {
    const clean = siteId.trim();
    if (!clean) return null;

    const url = getSupabaseUrl();
    const key = getSupabaseAnonKey();

    try {
      if (url && key) {
        const endpoint = `${url.replace(/\/$/, '')}/rest/v1/site_db?select=*&site_id=eq.${encodeURIComponent(clean)}&limit=1`;
        const response = await fetch(endpoint, {
          headers: {
            'apikey': key,
            'Authorization': `Bearer ${key}`
          }
        });

        if (response.ok) {
          const rows = await response.json();
          if (Array.isArray(rows) && rows.length > 0) {
            return mapRowToTelecomSite(rows[0], 0);
          }
        }
      }

      // Fallback search across database or cached list
      const searchRes = await this.searchSitesInDb(clean);
      const match = searchRes.sites.find(s => s.siteId.toLowerCase() === clean.toLowerCase());
      return match || null;
    } catch {
      return null;
    }
  },

  /**
   * Test Supabase connection and return diagnostics
   */
  async testConnection(): Promise<{ success: boolean; message: string; rowCount?: number }> {
    const url = getSupabaseUrl();
    const key = getSupabaseAnonKey();

    if (!url || !key) {
      return {
        success: false,
        message: 'SUPABASE_URL or SUPABASE_ANON_KEY is not configured in .env'
      };
    }

    try {
      const endpoint = `${url.replace(/\/$/, '')}/rest/v1/site_db?select=*&limit=5`;
      const response = await fetch(endpoint, {
        headers: {
          'apikey': key,
          'Authorization': `Bearer ${key}`,
          'Accept': 'application/json'
        }
      });

      if (!response.ok) {
        const errText = await response.text();
        return {
          success: false,
          message: `HTTP ${response.status} ${response.statusText}: ${errText}`
        };
      }

      const rows = await response.json();
      const count = Array.isArray(rows) ? rows.length : 0;
      return {
        success: true,
        message: count > 0 
          ? `Connected to Supabase PostgREST! Loaded ${count} sample site records from public.site_db table.`
          : `Connected to Supabase PostgREST (HTTP 200)! Table public.site_db is accessible. (If you have rows in the table, ensure an RLS SELECT policy is added in Supabase Table Editor: click "+ Add RLS policy" -> "Enable read access for all users").`,
        rowCount: count
      };
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      return {
        success: false,
        message: `Network/Fetch Error: ${msg}`
      };
    }
  }
};
