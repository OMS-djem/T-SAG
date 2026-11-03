import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { TelecomSite } from '../types';
import { TelemetryService } from '../services/telemetry';
import { 
  Search, 
  Plus, 
  MapPin, 
  Phone, 
  RadioTower, 
  Navigation, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  ExternalLink,
  X,
  RefreshCw
} from 'lucide-react';
import L from 'leaflet';

export const SitesMapScreen: React.FC = () => {
  const {
    gpsData,
    isRefreshingGps,
    refreshLocation,
    telecomSites,
    isSyncingSites,
    fetchSitesFromSupabase,
    selectedSiteForMap,
    selectSiteForMap,
    loadSiteIntoAudit,
    addNewTelecomSite
  } = useApp();

  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const technicianMarkerRef = useRef<L.LayerGroup | null>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);

  // New site form state
  const [newSite, setNewSite] = useState<Omit<TelecomSite, 'id'>>({
    siteId: '',
    siteName: '',
    operator: 'Mobiserve',
    latitude: gpsData.latitude,
    longitude: gpsData.longitude,
    altitude: gpsData.altitude,
    address: '',
    region: 'Algiers Central',
    siteType: 'Rooftop Tower',
    accessStatus: 'PENDING',
    accessInstructions: 'Security check-in at main entrance',
    contactPerson: '',
    contactPhone: ''
  });

  // Calculate distance for each site and filter by search
  const sortedAndFilteredSites = useMemo(() => {
    return telecomSites
      .map((site) => {
        const distance = TelemetryService.calculateDistanceMeters(
          gpsData.latitude,
          gpsData.longitude,
          site.latitude,
          site.longitude
        );
        return { ...site, distanceMeters: distance };
      })
      .filter((site) => {
        return (
          !searchQuery.trim() ||
          site.siteId.toLowerCase().includes(searchQuery.toLowerCase()) ||
          site.siteName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          site.operator.toLowerCase().includes(searchQuery.toLowerCase()) ||
          site.address.toLowerCase().includes(searchQuery.toLowerCase())
        );
      })
      .sort((a, b) => a.distanceMeters - b.distanceMeters);
  }, [telecomSites, gpsData.latitude, gpsData.longitude, searchQuery]);

  const nearestSite = sortedAndFilteredSites[0];

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [gpsData.latitude, gpsData.longitude],
        zoom: 13,
        zoomControl: false
      });

      L.control.zoom({ position: 'topright' }).addTo(map);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
      }).addTo(map);

      markersLayerRef.current = L.layerGroup().addTo(map);
      technicianMarkerRef.current = L.layerGroup().addTo(map);

      mapInstanceRef.current = map;
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // Update technician GPS location marker on map
  useEffect(() => {
    const map = mapInstanceRef.current;
    const techLayer = technicianMarkerRef.current;
    if (!map || !techLayer) return;

    techLayer.clearLayers();

    // Radar accuracy circle
    const accuracyCircle = L.circle([gpsData.latitude, gpsData.longitude], {
      radius: Math.max(gpsData.accuracy * 2, 40),
      color: '#0284C7',
      fillColor: '#38BDF8',
      fillOpacity: 0.18,
      weight: 1.5,
      className: 'radar-pulse'
    });
    techLayer.addLayer(accuracyCircle);

    // Technician pulse icon
    const techIcon = L.divIcon({
      className: 'custom-tech-pin',
      html: `
        <div style="position: relative; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">
          <div style="position: absolute; width: 24px; height: 24px; border-radius: 9999px; background-color: rgba(2, 132, 199, 0.4); animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;"></div>
          <div style="width: 14px; height: 14px; border-radius: 9999px; background-color: #0284C7; border: 2.5px solid #FFFFFF; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3);"></div>
        </div>
      `,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });

    const marker = L.marker([gpsData.latitude, gpsData.longitude], { icon: techIcon });
    marker.bindTooltip('<b>Field Technician</b><br/>Current GPS Position', { direction: 'top' });
    techLayer.addLayer(marker);
  }, [gpsData.latitude, gpsData.longitude, gpsData.accuracy]);

  // Update site markers
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layer = markersLayerRef.current;
    if (!map || !layer) return;

    layer.clearLayers();

    sortedAndFilteredSites.forEach((site) => {
      const color =
        site.accessStatus === 'ACCORDED' ? '#10B981' :
        site.accessStatus === 'REFUSED' ? '#EF4444' : '#F59E0B';

      const isSelected = selectedSiteForMap?.id === site.id;

      const towerIcon = L.divIcon({
        className: 'custom-site-pin',
        html: `
          <div style="
            background-color: ${color}; 
            color: white; 
            width: ${isSelected ? '36px' : '30px'}; 
            height: ${isSelected ? '36px' : '30px'}; 
            border-radius: 9999px; 
            border: 2.5px solid white; 
            box-shadow: 0 4px 10px rgba(0,0,0,0.35); 
            display: flex; 
            align-items: center; 
            justify-content: center;
            font-size: 13px;
            font-weight: 800;
            transition: all 0.2s ease;
          ">
            🗼
          </div>
        `,
        iconSize: [isSelected ? 36 : 30, isSelected ? 36 : 30],
        iconAnchor: [isSelected ? 18 : 15, isSelected ? 18 : 15]
      });

      const marker = L.marker([site.latitude, site.longitude], { icon: towerIcon });

      const popupHtml = `
        <div style="min-width: 200px; font-family: sans-serif; padding: 4px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
            <strong style="font-size: 13px; color: #0F1E36;">${site.siteId}</strong>
            <span style="font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 4px; background: ${color}20; color: ${color};">
              ${site.accessStatus}
            </span>
          </div>
          <div style="font-size: 12px; font-weight: 600; color: #334155;">${site.siteName}</div>
          <div style="font-size: 11px; color: #64748B; margin-bottom: 6px;">${site.operator} • ${site.siteType}</div>
          <div style="font-size: 11px; color: #475569; margin-bottom: 4px;">📍 ${site.address}</div>
          <div style="font-size: 11px; font-weight: 700; color: #0284C7; margin-bottom: 8px;">
            Radar Proximity: ${TelemetryService.formatDistance(site.distanceMeters)} away
          </div>
          <button 
            id="audit-site-btn-${site.id}" 
            style="width: 100%; padding: 6px 10px; background-color: #0F1E36; color: white; border: none; border-radius: 6px; font-size: 11px; font-weight: bold; cursor: pointer;"
          >
            Audit This Site →
          </button>
        </div>
      `;

      marker.bindPopup(popupHtml);

      marker.on('popupopen', () => {
        selectSiteForMap(site);
        setTimeout(() => {
          const btn = document.getElementById(`audit-site-btn-${site.id}`);
          if (btn) {
            btn.onclick = () => loadSiteIntoAudit(site);
          }
        }, 50);
      });

      layer.addLayer(marker);
    });
  }, [sortedAndFilteredSites, selectedSiteForMap, selectSiteForMap, loadSiteIntoAudit]);

  const handleAddSiteSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSite.siteId.trim() || !newSite.siteName.trim()) return;

    addNewTelecomSite(newSite);
    setShowAddModal(false);
    setNewSite({
      siteId: '',
      siteName: '',
      operator: 'Mobiserve',
      latitude: gpsData.latitude,
      longitude: gpsData.longitude,
      altitude: gpsData.altitude,
      address: '',
      region: 'Algiers Central',
      siteType: 'Rooftop Tower',
      accessStatus: 'PENDING',
      accessInstructions: 'Security check-in at main entrance',
      contactPerson: '',
      contactPhone: ''
    });
  };

  return (
    <div className="relative w-full h-[calc(100vh-112px)] overflow-hidden flex flex-col">
      {/* 1. Top Controls Overlay */}
      <div className="absolute top-3 left-3 right-3 z-20 flex flex-col gap-2 max-w-xl mx-auto pointer-events-auto">
        <div className="bg-white/95 backdrop-blur-md rounded-2xl shadow-lg border border-slate-200/80 p-2.5 flex items-center space-x-2">
          <Search className="w-4 h-4 text-slate-400 ml-1.5 shrink-0" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search site ID, name, operator, or city..."
            className="w-full text-xs bg-transparent focus:outline-hidden text-slate-800 placeholder-slate-400"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="p-1 text-slate-400 hover:text-slate-600 rounded-full"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
          <button
            type="button"
            disabled={isSyncingSites}
            onClick={() => fetchSitesFromSupabase(true)}
            className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center shrink-0 transition-colors disabled:opacity-50"
            title="Sync sites from Supabase site_db"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-emerald-600 ${isSyncingSites ? 'animate-spin' : ''}`} />
          </button>
          <button
            type="button"
            onClick={() => setShowAddModal(true)}
            className="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold flex items-center shrink-0 shadow-sm transition-colors"
          >
            <Plus className="w-3.5 h-3.5 mr-1" /> Add Site
          </button>
        </div>
      </div>

      {/* 2. Map Canvas */}
      <div ref={mapContainerRef} className="flex-1 w-full h-full z-10" />

      {/* 3. Floating Bottom Radar Proximity Card */}
      {nearestSite && (
        <div className="absolute bottom-4 left-3 right-3 sm:left-auto sm:right-4 sm:w-96 z-20 pointer-events-auto">
          <div className="bg-white/95 backdrop-blur-md rounded-2xl p-4 shadow-2xl border border-slate-200/90 text-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Navigation className="w-4 h-4 text-blue-600" />
                <span className="text-[11px] font-black uppercase text-blue-600 tracking-wider">
                  Nearest Telecom Site Radar
                </span>
              </div>
              <span className={`text-[10px] font-black px-2 py-0.5 rounded ${
                nearestSite.accessStatus === 'ACCORDED' ? 'bg-emerald-100 text-emerald-800' :
                nearestSite.accessStatus === 'REFUSED' ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'
              }`}>
                {nearestSite.accessStatus}
              </span>
            </div>

            <div className="flex justify-between items-baseline">
              <h4 className="text-sm font-bold text-slate-900">{nearestSite.siteId} - {nearestSite.siteName}</h4>
              <span className="text-xs font-black text-sky-700">
                {TelemetryService.formatDistance(nearestSite.distanceMeters)}
              </span>
            </div>

            <p className="text-xs text-slate-500 truncate">{nearestSite.address}</p>

            <div className="pt-2 flex space-x-2">
              <button
                onClick={() => {
                  if (mapInstanceRef.current) {
                    mapInstanceRef.current.setView([nearestSite.latitude, nearestSite.longitude], 16);
                  }
                  selectSiteForMap(nearestSite);
                }}
                className="flex-1 py-2 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition-colors"
              >
                Center Map
              </button>
              <button
                onClick={() => loadSiteIntoAudit(nearestSite)}
                className="flex-1 py-2 px-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold transition-colors shadow-sm"
              >
                Audit This Site →
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 4. Add Site Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl text-slate-800 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4 border-b border-slate-100 pb-3">
              <div className="flex items-center space-x-2">
                <RadioTower className="w-5 h-5 text-blue-600" />
                <h3 className="text-base font-bold text-slate-900">Add Telecom Field Site</h3>
              </div>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1 rounded-full text-slate-400 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddSiteSubmit} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-600 mb-1">Site ID</label>
                  <input
                    type="text"
                    required
                    value={newSite.siteId}
                    onChange={(e) => setNewSite({ ...newSite, siteId: e.target.value })}
                    placeholder="e.g. MOB-ALG-9901"
                    className="w-full font-mono font-bold px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-600 mb-1">Operator</label>
                  <select
                    value={newSite.operator}
                    onChange={(e) => setNewSite({ ...newSite, operator: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  >
                    <option value="Mobiserve">Mobiserve</option>
                    <option value="Djezzy">Djezzy</option>
                    <option value="Mobilis">Mobilis</option>
                    <option value="Ooredoo">Ooredoo</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-600 mb-1">Site Name</label>
                <input
                  type="text"
                  required
                  value={newSite.siteName}
                  onChange={(e) => setNewSite({ ...newSite, siteName: e.target.value })}
                  placeholder="e.g. Birkhadem Regional Monopole"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-600 mb-1">Latitude</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={newSite.latitude}
                    onChange={(e) => setNewSite({ ...newSite, latitude: parseFloat(e.target.value) || 0 })}
                    className="w-full font-mono px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-600 mb-1">Longitude</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={newSite.longitude}
                    onChange={(e) => setNewSite({ ...newSite, longitude: parseFloat(e.target.value) || 0 })}
                    className="w-full font-mono px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-600 mb-1">Site Type</label>
                <select
                  value={newSite.siteType}
                  onChange={(e) => setNewSite({ ...newSite, siteType: e.target.value as TelecomSite['siteType'] })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                >
                  <option value="Rooftop Tower">Rooftop Tower</option>
                  <option value="Monopole">Monopole</option>
                  <option value="Greenfield Tower">Greenfield Tower</option>
                  <option value="Greenfield Mast">Greenfield Mast</option>
                  <option value="Indoor Microcell">Indoor Microcell</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-600 mb-1">Address / Landmark</label>
                <input
                  type="text"
                  value={newSite.address}
                  onChange={(e) => setNewSite({ ...newSite, address: e.target.value })}
                  placeholder="Street address, city, landmark"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-600 mb-1">Contact Person</label>
                  <input
                    type="text"
                    value={newSite.contactPerson}
                    onChange={(e) => setNewSite({ ...newSite, contactPerson: e.target.value })}
                    placeholder="Site guard / Landlord"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-600 mb-1">Contact Phone</label>
                  <input
                    type="text"
                    value={newSite.contactPhone}
                    onChange={(e) => setNewSite({ ...newSite, contactPhone: e.target.value })}
                    placeholder="+213 550..."
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-sm"
                >
                  Save Site to Directory
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
