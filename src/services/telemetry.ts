import { GpsData, GsmData } from '../types';

// Default Algeria Telecommunications Field Center (Hydra, Algiers)
const DEFAULT_LAT = 36.754800;
const DEFAULT_LNG = 3.056900;

export const TelemetryService = {
  async getCurrentLocation(): Promise<GpsData> {
    return new Promise((resolve) => {
      if (!('geolocation' in navigator)) {
        resolve(this.getMockGps(DEFAULT_LAT, DEFAULT_LNG));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = Number(pos.coords.latitude.toFixed(6));
          const lng = Number(pos.coords.longitude.toFixed(6));
          const alt = pos.coords.altitude ? Number(pos.coords.altitude.toFixed(1)) : 48.0;
          const acc = pos.coords.accuracy ? Number(pos.coords.accuracy.toFixed(1)) : 3.5;
          const coordsStr = `${lat >= 0 ? '+' : ''}${lat.toFixed(6)}, ${lng >= 0 ? '+' : ''}${lng.toFixed(6)}`;

          resolve({
            latitude: lat,
            longitude: lng,
            altitude: alt,
            accuracy: acc,
            address: 'Algiers Hydra Telecom District, Algeria',
            timestamp: pos.timestamp || Date.now(),
            coordinatesString: coordsStr,
            mapsUrl: `https://maps.google.com/?q=${lat},${lng}`
          });
        },
        (_err) => {
          // Fallback to field site coordinate with jitter
          const jitterLat = Number((DEFAULT_LAT + (Math.random() - 0.5) * 0.002).toFixed(6));
          const jitterLng = Number((DEFAULT_LNG + (Math.random() - 0.5) * 0.002).toFixed(6));
          resolve(this.getMockGps(jitterLat, jitterLng));
        },
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      );
    });
  },

  getMockGps(lat: number, lng: number): GpsData {
    const coordsStr = `${lat >= 0 ? '+' : ''}${lat.toFixed(6)}, ${lng >= 0 ? '+' : ''}${lng.toFixed(6)}`;
    return {
      latitude: lat,
      longitude: lng,
      altitude: 48.0,
      accuracy: 3.2,
      address: 'Rue Djenane El Malik, Hydra, Algiers',
      timestamp: Date.now(),
      coordinatesString: coordsStr,
      mapsUrl: `https://maps.google.com/?q=${lat},${lng}`
    };
  },

  getGsmTelemetry(operatorPreference = 'Mobiserve / Djezzy'): GsmData {
    // Generate realistic GSM/LTE/5G telemetry matching Algerian Telecom standards
    // MCC 603 = Algeria. 01 = Mobilis, 02 = Djezzy, 03 = Ooredoo
    let mcc = '603';
    let mnc = '02';
    let opName = 'Djezzy GSM';
    let networkType = 'LTE / 4G+';

    if (operatorPreference.toLowerCase().includes('mobilis')) {
      mnc = '01';
      opName = 'Mobilis DZ';
      networkType = '5G NR';
    } else if (operatorPreference.toLowerCase().includes('ooredoo')) {
      mnc = '03';
      opName = 'Ooredoo DZ';
      networkType = 'LTE / 4G';
    } else if (operatorPreference.toLowerCase().includes('mobiserve')) {
      mnc = '02';
      opName = 'Mobiserve / Djezzy Partner Net';
      networkType = 'LTE / 4G+';
    }

    const cellId = String(Math.floor(1000000 + Math.random() * 9000000));
    const lacTac = String(Math.floor(1000 + Math.random() * 8999));
    const dbm = -65 - Math.floor(Math.random() * 35); // -65 to -100 dBm

    let signalLevel = 'Excellent';
    if (dbm < -95) signalLevel = 'Poor';
    else if (dbm < -85) signalLevel = 'Fair';
    else if (dbm < -75) signalLevel = 'Good';

    return {
      operatorName: opName,
      networkType,
      cellId,
      lacTac,
      mcc,
      mnc,
      signalDbm: dbm,
      signalLevel,
      frequencyMhz: 1800 + Math.floor(Math.random() * 800)
    };
  },

  calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371000; // Earth radius in meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c);
  },

  formatDistance(meters: number): string {
    if (meters < 1000) {
      return `${meters} m`;
    }
    return `${(meters / 1000).toFixed(1)} km`;
  }
};
