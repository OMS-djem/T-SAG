import { TelecomSite } from '../types';

// Retrieve Supabase credentials from environment
const getEnvVar = (key: string): string => {
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    const metaVal = (import.meta.env as Record<string, string | undefined>)[key] || 
                    (import.meta.env as Record<string, string | undefined>)[`VITE_${key}`];
    if (metaVal) return String(metaVal);
  }
  if (typeof process !== 'undefined' && process.env) {
    const procVal = process.env[key] || process.env[`VITE_${key}`];
    if (procVal) return String(procVal);
  }
  return '';
};

export const SUPABASE_URL = 
  getEnvVar('SUPABASE_URL') || 
  'https://tsag-telecom-site.supabase.co';

export const SUPABASE_ANON_KEY = 
  getEnvVar('SUPABASE_ANON_KEY') || 
  '';

export const isSupabaseConfigured = (): boolean => {
  return Boolean(
    SUPABASE_URL && 
    SUPABASE_URL.startsWith('http') && 
    SUPABASE_ANON_KEY && 
    SUPABASE_ANON_KEY.length > 20 &&
    !SUPABASE_ANON_KEY.includes('dummy_anon_key')
  );
};

// Normalize rows from site_db table into TelecomSite format
export function mapRowToTelecomSite(row: Record<string, unknown>, index: number): TelecomSite {
  const getStr = (keys: string[]): string => {
    for (const k of keys) {
      if (row[k] !== undefined && row[k] !== null && String(row[k]).trim() !== '') {
        return String(row[k]).trim();
      }
    }
    return '';
  };

  const getNum = (keys: string[], defaultVal: number): number => {
    for (const k of keys) {
      if (row[k] !== undefined && row[k] !== null) {
        const parsed = parseFloat(String(row[k]));
        if (!isNaN(parsed)) return parsed;
      }
    }
    return defaultVal;
  };

  const siteId = getStr(['site_id', 'siteId', 'code_site', 'site', 'id']) || `SITE-${index + 1}`;
  const siteName = getStr(['site_name', 'siteName', 'name', 'nom_site', 'nom']) || siteId;
  const operator = getStr(['operator', 'operateur', 'carrier', 'network']) || 'Mobiserve';
  const latitude = getNum(['latitude', 'lat', 'lat_dd', 'coord_lat'], 36.754800);
  const longitude = getNum(['longitude', 'lng', 'lon', 'long', 'coord_lng'], 3.056900);
  const altitude = getNum(['altitude', 'alt', 'hauteur', 'elevation'], 45.0);
  const address = getStr(['address', 'adresse', 'location', 'commune', 'wilaya']) || 'Algiers, Algeria';
  const region = getStr(['region', 'wilaya', 'zone', 'area']) || 'National';

  const rawType = getStr(['site_type', 'siteType', 'type', 'typologie']);
  let siteType: TelecomSite['siteType'] = 'Rooftop Tower';
  if (/monopole/i.test(rawType)) siteType = 'Monopole';
  else if (/microcell|indoor/i.test(rawType)) siteType = 'Indoor Microcell';
  else if (/mast/i.test(rawType)) siteType = 'Greenfield Mast';
  else if (/greenfield/i.test(rawType)) siteType = 'Greenfield Tower';

  const rawStatus = getStr(['access_status', 'accessStatus', 'status', 'statut']).toUpperCase();
  let accessStatus: TelecomSite['accessStatus'] = 'PENDING';
  if (rawStatus.includes('ACCORD') || rawStatus.includes('GRANT') || rawStatus === 'OK') {
    accessStatus = 'ACCORDED';
  } else if (rawStatus.includes('REFUS') || rawStatus.includes('DENI') || rawStatus.includes('NO')) {
    accessStatus = 'REFUSED';
  }

  const refusalReason = getStr(['refusal_reason', 'refusalReason', 'motif_refus', 'reason']) || undefined;
  const accessInstructions = getStr(['access_instructions', 'accessInstructions', 'instructions', 'consignes']) || 
    'Access badge & ID required at security post';
  const contactPerson = getStr(['contact_person', 'contactPerson', 'contact', 'responsable', 'site_manager']);
  const contactPhone = getStr(['contact_phone', 'contactPhone', 'phone', 'tel', 'telephone']);

  return {
    id: String(row.id || `site_${siteId}_${index}`),
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
  async fetchSitesFromSiteDb(): Promise<{ sites: TelecomSite[]; source: 'supabase' | 'fallback'; error?: string }> {
    if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
      return { 
        sites: [], 
        source: 'fallback', 
        error: 'SUPABASE_URL or SUPABASE_ANON_KEY missing in environment' 
      };
    }

    try {
      const endpoint = `${SUPABASE_URL.replace(/\/$/, '')}/rest/v1/site_db?select=*`;
      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'apikey': SUPABASE_ANON_KEY,
          'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
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
        return { sites: mapped, source: 'supabase' };
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
  }
};
