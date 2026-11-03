import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { 
  RadioTower, 
  MapPin, 
  Navigation, 
  RefreshCw, 
  Signal, 
  Camera, 
  Trash2, 
  ZoomIn, 
  Send, 
  Eye, 
  Save, 
  CheckCircle2, 
  XCircle, 
  ExternalLink,
  ChevronDown
} from 'lucide-react';

const COMMON_REFUSAL_REASONS = [
  'Site security guard requested regional work permit copy',
  'Building syndicate refused roof access without 48h advance notice',
  'Access padlocks replaced without NOC key distribution',
  'Civilian protests or hostile landlord dispute near tower',
  'Severe weather / flash flooding rendering access track impassable',
  'Military liaison / restricted perimeter security escort missing',
  'Power room padlocked by local municipality electricity company'
];

export const SiteAuditScreen: React.FC = () => {
  const {
    siteId, setSiteId,
    siteName, setSiteName,
    operator, setOperator,
    status, setStatus,
    refusalReason, setRefusalReason,
    notes, setNotes,
    technicianName, setTechnicianName,
    technicianPhone, setTechnicianPhone,
    recipientEmail, setRecipientEmail,
    ccRecipientsEmail, setCcRecipientsEmail,
    gpsData, isRefreshingGps, refreshLocation,
    gsmData, isRefreshingGsm, refreshGsm,
    photos, removePhoto, openCamera,
    setShowEmailPreview,
    isSendingEmail,
    sendAuditReport,
    saveAsDraftQueue,
    telecomSites,
    setActiveTab
  } = useApp();

  const [previewPhoto, setPreviewPhoto] = useState<string | null>(null);
  const [showSitePicker, setShowSitePicker] = useState(false);

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 space-y-6 pb-24">
      {/* 1. Brand Hero Header */}
      <div className="bg-gradient-to-r from-telecom-navy via-slate-900 to-slate-800 rounded-2xl p-5 text-white shadow-xl border border-slate-700/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3.5">
          <div className="w-12 h-12 rounded-2xl bg-blue-600/30 border border-blue-400/40 flex items-center justify-center text-sky-400 shrink-0 shadow-inner">
            <RadioTower className="w-7 h-7" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-[11px] uppercase tracking-wider text-sky-400 font-black">Mobiserve Holding</span>
              <span className="bg-sky-500/20 text-sky-300 text-[10px] font-bold px-2 py-0.5 rounded-full border border-sky-400/30">
                T-SAG Field v2.4
              </span>
            </div>
            <h2 className="text-lg font-black text-white leading-tight">
              Telecom Site Access & Audit Dispatch
            </h2>
            <p className="text-xs text-slate-300 mt-0.5">
              Cellular GSM telemetry, verified GPS coordinates, and stamped photo logs
            </p>
          </div>
        </div>

        <button
          onClick={() => setActiveTab('MAP')}
          className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold text-white flex items-center transition-colors shrink-0"
        >
          <Navigation className="w-3.5 h-3.5 mr-1.5 text-sky-400" />
          View Radar & Sites Map
        </button>
      </div>

      {/* 2. Site Identification Form */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <RadioTower className="w-5 h-5 text-blue-600" />
            <h3 className="text-sm font-bold text-slate-800">Site Information</h3>
          </div>

          <div className="relative">
            <button
              type="button"
              onClick={() => setShowSitePicker(!showSitePicker)}
              className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg transition-colors"
            >
              Select from Directory <ChevronDown className="w-3.5 h-3.5 ml-1" />
            </button>

            {showSitePicker && (
              <div className="absolute right-0 top-full mt-1 w-72 bg-white rounded-xl shadow-2xl border border-slate-200 z-40 max-h-60 overflow-y-auto p-1 text-xs">
                {telecomSites.map((site) => (
                  <button
                    key={site.id}
                    type="button"
                    onClick={() => {
                      setSiteId(site.siteId);
                      setSiteName(site.siteName);
                      setOperator(site.operator);
                      if (site.accessStatus === 'ACCORDED') setStatus('ACCORDED');
                      if (site.accessStatus === 'REFUSED') {
                        setStatus('REFUSED');
                        setRefusalReason(site.refusalReason || 'Site access refused');
                      }
                      setShowSitePicker(false);
                    }}
                    className="w-full text-left p-2 hover:bg-slate-50 rounded-lg flex flex-col transition-colors border-b border-slate-100 last:border-b-0"
                  >
                    <div className="flex justify-between items-center">
                      <span className="font-bold text-slate-900">{site.siteId}</span>
                      <span className={`text-[10px] font-black px-1.5 py-0.5 rounded ${
                        site.accessStatus === 'ACCORDED' ? 'bg-emerald-100 text-emerald-700' :
                        site.accessStatus === 'REFUSED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'
                      }`}>
                        {site.accessStatus}
                      </span>
                    </div>
                    <span className="text-slate-600 text-[11px] truncate">{site.siteName}</span>
                    <span className="text-slate-400 text-[10px]">{site.operator} • {site.region}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Site ID</label>
            <input
              type="text"
              value={siteId}
              onChange={(e) => setSiteId(e.target.value)}
              placeholder="e.g. MOB-ALG-1044"
              className="w-full text-sm font-mono font-bold px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Site Name</label>
            <input
              type="text"
              value={siteName}
              onChange={(e) => setSiteName(e.target.value)}
              placeholder="e.g. Hydra Telecom Tower"
              className="w-full text-sm px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Operator</label>
            <input
              type="text"
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="e.g. Mobiserve / Djezzy"
              className="w-full text-sm px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>
        </div>
      </div>

      {/* 3. Site Access Status Selector Card */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
        <h3 className="text-sm font-bold text-slate-800">Site Access Audit Verdict</h3>

        <div className="grid grid-cols-2 gap-3">
          {/* Accorded Button */}
          <button
            type="button"
            onClick={() => setStatus('ACCORDED')}
            className={`p-4 rounded-2xl border-2 flex flex-col items-center justify-center text-center transition-all ${
              status === 'ACCORDED'
                ? 'border-emerald-600 bg-emerald-50 text-emerald-900 shadow-md ring-2 ring-emerald-400/30'
                : 'border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100'
            }`}
          >
            <CheckCircle2 className={`w-8 h-8 mb-1.5 ${status === 'ACCORDED' ? 'text-emerald-600' : 'text-slate-400'}`} />
            <span className="text-sm font-black">ACCESS ACCORDED</span>
            <span className="text-[11px] text-slate-500 mt-0.5">Technician Granted Full Entry ✓</span>
          </button>

          {/* Refused Button */}
          <button
            type="button"
            onClick={() => setStatus('REFUSED')}
            className={`p-4 rounded-2xl border-2 flex flex-col items-center justify-center text-center transition-all ${
              status === 'REFUSED'
                ? 'border-red-600 bg-red-50 text-red-900 shadow-md ring-2 ring-red-400/30'
                : 'border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100'
            }`}
          >
            <XCircle className={`w-8 h-8 mb-1.5 ${status === 'REFUSED' ? 'text-red-600' : 'text-slate-400'}`} />
            <span className="text-sm font-black">NO ACCESS (REFUSED)</span>
            <span className="text-[11px] text-slate-500 mt-0.5">Access Blocked / Denied ✕</span>
          </button>
        </div>

        {/* Refusal Reason Dropdown if Refused */}
        {status === 'REFUSED' && (
          <div className="bg-red-50/70 border border-red-200 rounded-xl p-4 space-y-3 animate-in fade-in">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-red-900">
                Primary Reason for Access Refusal
              </label>
              <span className="text-[11px] font-semibold text-red-700 bg-red-100 px-2 py-0.5 rounded">
                Required for Incident Escalation
              </span>
            </div>

            <select
              value={refusalReason}
              onChange={(e) => setRefusalReason(e.target.value)}
              className="w-full text-xs font-medium px-3 py-2.5 bg-white border border-red-300 rounded-xl text-slate-800 focus:ring-2 focus:ring-red-500 focus:outline-hidden"
            >
              {COMMON_REFUSAL_REASONS.map((reason) => (
                <option key={reason} value={reason}>
                  {reason}
                </option>
              ))}
            </select>

            <input
              type="text"
              value={refusalReason}
              onChange={(e) => setRefusalReason(e.target.value)}
              placeholder="Or enter custom refusal reason..."
              className="w-full text-xs px-3 py-2 bg-white border border-red-300 rounded-xl text-slate-800 focus:ring-2 focus:ring-red-500 focus:outline-hidden"
            />
          </div>
        )}

        {/* Field Notes & Observations */}
        <div>
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Work Notes & Site Observations
          </label>
          <textarea
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Document access permit details, padlock status, equipment room condition, or escalation comments..."
            className="w-full text-xs px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden leading-relaxed"
          />
        </div>
      </div>

      {/* 4. Real-time Telemetry: GPS & GSM Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* GPS Location Card */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-3 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-2">
                <MapPin className="w-5 h-5 text-blue-600" />
                <h4 className="text-sm font-bold text-slate-800">GPS Coordinates Stamp</h4>
              </div>
              <button
                type="button"
                onClick={refreshLocation}
                disabled={isRefreshingGps}
                className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
                title="Refresh GPS Coordinates"
              >
                <RefreshCw className={`w-4 h-4 ${isRefreshingGps ? 'animate-spin text-blue-600' : ''}`} />
              </button>
            </div>

            <div className="bg-slate-900 text-white rounded-xl p-3.5 space-y-1 font-mono text-xs border border-slate-800">
              <div className="flex justify-between items-center text-sky-400 font-bold">
                <span>COORDINATES:</span>
                <span className="text-white text-xs">{gpsData.coordinatesString}</span>
              </div>
              <div className="flex justify-between text-slate-300 text-[11px]">
                <span>ACCURACY:</span>
                <span className="text-emerald-400 font-bold">±{gpsData.accuracy} m</span>
              </div>
              <div className="flex justify-between text-slate-300 text-[11px]">
                <span>ALTITUDE:</span>
                <span>{gpsData.altitude} m MSL</span>
              </div>
              <div className="pt-1 border-t border-slate-800 text-[10px] text-slate-400 truncate">
                {gpsData.address}
              </div>
            </div>
          </div>

          <a
            href={gpsData.mapsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center justify-center pt-1"
          >
            Open in Google Maps <ExternalLink className="w-3.5 h-3.5 ml-1" />
          </a>
        </div>

        {/* GSM & Cellular Telemetry Card */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-3 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-2">
                <Signal className="w-5 h-5 text-indigo-600" />
                <h4 className="text-sm font-bold text-slate-800">Cellular & GSM Telemetry</h4>
              </div>
              <button
                type="button"
                onClick={refreshGsm}
                disabled={isRefreshingGsm}
                className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
                title="Refresh Cell Network Telemetry"
              >
                <RefreshCw className={`w-4 h-4 ${isRefreshingGsm ? 'animate-spin text-indigo-600' : ''}`} />
              </button>
            </div>

            <div className="bg-slate-900 text-white rounded-xl p-3.5 space-y-1 font-mono text-xs border border-slate-800">
              <div className="flex justify-between items-center text-indigo-300 font-bold">
                <span>CONNECTED:</span>
                <span className="bg-indigo-950 text-indigo-300 px-2 py-0.5 rounded border border-indigo-700/50 text-[10px]">
                  {gsmData.networkType}
                </span>
              </div>
              <div className="flex justify-between text-slate-300 text-[11px]">
                <span>OPERATOR:</span>
                <span className="text-white font-semibold">{gsmData.operatorName}</span>
              </div>
              <div className="flex justify-between text-slate-300 text-[11px]">
                <span>CELL ID (CID):</span>
                <span className="text-amber-400 font-bold">{gsmData.cellId}</span>
              </div>
              <div className="flex justify-between text-slate-300 text-[11px]">
                <span>LAC / TAC:</span>
                <span>{gsmData.lacTac} (MCC-MNC {gsmData.mcc}-{gsmData.mnc})</span>
              </div>
              <div className="flex justify-between text-slate-300 text-[11px] pt-1 border-t border-slate-800">
                <span>SIGNAL STRENGTH:</span>
                <span className="text-emerald-400 font-bold">{gsmData.signalDbm} dBm ({gsmData.signalLevel})</span>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
            <span>Carrier Band: Band 3 (1800 MHz)</span>
            <span className="text-emerald-600 font-bold flex items-center">
              ● RSRP Synchronized
            </span>
          </div>
        </div>
      </div>

      {/* 5. Stamped Photo Evidence Section */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <Camera className="w-5 h-5 text-blue-600" />
            <h3 className="text-sm font-bold text-slate-800">
              Stamped Photo Evidence ({photos.length})
            </h3>
          </div>

          <button
            type="button"
            onClick={() => openCamera('Site Entrance')}
            className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold flex items-center shadow-md transition-colors"
          >
            <Camera className="w-4 h-4 mr-1.5" /> Take Stamped Photo
          </button>
        </div>

        {photos.length === 0 ? (
          <div 
            onClick={() => openCamera('Site Entrance')}
            className="border-2 border-dashed border-slate-300 rounded-2xl p-8 text-center cursor-pointer hover:bg-slate-50 transition-colors"
          >
            <Camera className="w-10 h-10 text-slate-400 mx-auto mb-2" />
            <h4 className="text-sm font-bold text-slate-700">No Photo Evidence Attached</h4>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Click to capture site entrance, equipment shelter, tower structure, or locked padlock. Photos are automatically stamped with GPS, GSM CID, and security header.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {photos.map((photo) => (
              <div 
                key={photo.id}
                className="group relative bg-slate-900 rounded-xl overflow-hidden border border-slate-200 aspect-4/3 shadow-xs"
              >
                <img
                  src={photo.dataUrl}
                  alt={photo.tag}
                  className="w-full h-full object-cover"
                />
                
                {/* Overlay Tag Badge */}
                <span className="absolute top-1.5 left-1.5 bg-slate-950/80 backdrop-blur-xs text-white text-[10px] font-bold px-2 py-0.5 rounded">
                  {photo.tag}
                </span>

                {/* Actions Hover Strip */}
                <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center space-x-3">
                  <button
                    type="button"
                    onClick={() => setPreviewPhoto(photo.dataUrl)}
                    className="p-2 rounded-full bg-white/20 hover:bg-white text-white hover:text-slate-900 transition-colors"
                    title="Zoom Watermarked Photo"
                  >
                    <ZoomIn className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => removePhoto(photo.id)}
                    className="p-2 rounded-full bg-red-600/80 hover:bg-red-600 text-white transition-colors"
                    title="Remove Photo"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 6. Email Dispatch & Reporting Details */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
        <h3 className="text-sm font-bold text-slate-800">Zoho Mail Dispatch & Routing</h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
          <div>
            <label className="block font-semibold text-slate-600 mb-1">Field Technician</label>
            <input
              type="text"
              value={technicianName}
              onChange={(e) => setTechnicianName(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-600 mb-1">Technician Phone</label>
            <input
              type="text"
              value={technicianPhone}
              onChange={(e) => setTechnicianPhone(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-600 mb-1">Primary Zoho Recipient</label>
            <input
              type="email"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-600 mb-1">CC Group (NOC & Supervision)</label>
            <input
              type="text"
              value={ccRecipientsEmail}
              onChange={(e) => setCcRecipientsEmail(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            />
          </div>
        </div>

        {/* Primary Action Buttons */}
        <div className="pt-2 flex flex-col sm:flex-row gap-3">
          <button
            type="button"
            onClick={() => setShowEmailPreview(true)}
            className="flex-1 py-3 px-4 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center justify-center transition-colors"
          >
            <Eye className="w-4 h-4 mr-2 text-slate-500" /> Preview Zoho Email
          </button>

          <button
            type="button"
            onClick={saveAsDraftQueue}
            className="flex-1 py-3 px-4 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center justify-center transition-colors"
          >
            <Save className="w-4 h-4 mr-2 text-slate-500" /> Save Offline Queue
          </button>

          <button
            type="button"
            disabled={isSendingEmail}
            onClick={sendAuditReport}
            className={`flex-1 py-3 px-4 rounded-xl text-white text-xs font-black flex items-center justify-center shadow-lg transition-all ${
              status === 'ACCORDED' 
                ? 'bg-emerald-600 hover:bg-emerald-700' 
                : 'bg-red-600 hover:bg-red-700'
            } disabled:opacity-50`}
          >
            {isSendingEmail ? (
              <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Send className="w-4 h-4 mr-2" />
            )}
            {status === 'ACCORDED' ? 'Dispatch Accorded Report' : 'Dispatch Refusal Alert'}
          </button>
        </div>
      </div>

      {/* Photo Enlarge Modal */}
      {previewPhoto && (
        <div 
          onClick={() => setPreviewPhoto(null)}
          className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4 cursor-pointer"
        >
          <div className="relative max-w-4xl max-h-[90vh] overflow-hidden rounded-xl">
            <img
              src={previewPhoto}
              alt="Watermarked Preview"
              className="w-full h-full object-contain rounded-xl"
            />
            <button
              onClick={() => setPreviewPhoto(null)}
              className="absolute top-3 right-3 bg-black/70 text-white rounded-full p-2 text-xs font-bold"
            >
              ✕ Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
