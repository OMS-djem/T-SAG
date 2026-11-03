export type UserRoleCode = 'ADMIN' | 'OM' | 'SUPERVISION' | 'TECHNICIAN' | 'DISPATCHER' | 'ACQUISITION';

export interface UserRoleInfo {
  code: UserRoleCode;
  label: string;
  description: string;
  color: string;
}

export const USER_ROLES: Record<UserRoleCode, UserRoleInfo> = {
  ADMIN: {
    code: 'ADMIN',
    label: 'Administrator',
    description: 'Full system configuration, security & user management',
    color: '#DC2626' // Red
  },
  OM: {
    code: 'OM',
    label: 'O&M',
    description: 'Operations & Maintenance field engineer',
    color: '#D97706' // Amber
  },
  SUPERVISION: {
    code: 'SUPERVISION',
    label: 'Supervision',
    description: 'NOC & regional supervisor monitoring site access',
    color: '#7C3AED' // Purple
  },
  TECHNICIAN: {
    code: 'TECHNICIAN',
    label: 'Technician',
    description: 'Field technician executing audits and photo logging',
    color: '#2563EB' // Blue
  },
  DISPATCHER: {
    code: 'DISPATCHER',
    label: 'Dispatcher',
    description: 'Work order & site access routing coordinator',
    color: '#059669' // Green
  },
  ACQUISITION: {
    code: 'ACQUISITION',
    label: 'Acquisition',
    description: 'Site acquisition & landlord lease clearance officer',
    color: '#0891B2' // Cyan
  }
};

export type SiteAccessStatus = 'ACCORDED' | 'REFUSED';

export interface GpsData {
  latitude: number;
  longitude: number;
  altitude: number;
  accuracy: number;
  address: string;
  timestamp: number;
  coordinatesString: string;
  mapsUrl: string;
}

export interface GsmData {
  operatorName: string;
  networkType: string; // LTE / 4G, 5G NR, 3G HSPA+, 2G GSM
  cellId: string;
  lacTac: string;
  mcc: string;
  mnc: string;
  signalDbm: number;
  signalLevel: string; // Excellent, Good, Fair, Poor
  frequencyMhz?: number;
}

export interface SitePhoto {
  id: string;
  dataUrl: string; // Base64 data URL
  tag: string;
  timestamp: number;
  latitude: number;
  longitude: number;
  cellId: string;
}

export interface TelecomSite {
  id: string;
  siteId: string;
  siteName: string;
  operator: string;
  latitude: number;
  longitude: number;
  altitude: number;
  address: string;
  region: string;
  siteType: 'Rooftop Tower' | 'Monopole' | 'Greenfield Tower' | 'Greenfield Mast' | 'Indoor Microcell';
  accessStatus: 'ACCORDED' | 'REFUSED' | 'PENDING';
  refusalReason?: string;
  accessInstructions: string;
  contactPerson: string;
  contactPhone: string;
  lastAuditTimestamp?: number;
}

export interface SiteReport {
  id: string;
  siteId: string;
  siteName: string;
  operator: string;
  status: SiteAccessStatus;
  refusalReason?: string;
  technicianName: string;
  technicianPhone: string;
  technicianEmail: string;
  recipientEmail: string;
  ccRecipientsEmail?: string;
  notes: string;
  gps: GpsData;
  gsm: GsmData;
  photos: SitePhoto[];
  createdAt: number;
  syncStatus: 'PENDING' | 'SYNCED' | 'FAILED';
  syncErrorMessage?: string;
  syncedAt?: number;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: UserRoleCode;
  department: string;
  region: string;
  isActive: boolean;
  createdAt: string;
}

export interface MailConfig {
  senderEmail: string;
  senderName: string;
  toGroup: string;
  ccGroup: string;
  host: string;
  port: number;
  password?: string;
}
