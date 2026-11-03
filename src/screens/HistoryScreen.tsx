import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { SiteReport } from '../types';
import { EmailService } from '../services/email';
import { 
  History, 
  Wifi, 
  WifiOff, 
  RefreshCw, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  MapPin, 
  Signal, 
  User, 
  Copy, 
  Check, 
  Trash2, 
  Map, 
  RadioTower, 
  ZoomIn, 
  FileText
} from 'lucide-react';

export const HistoryScreen: React.FC = () => {
  const {
    reports,
    isOnline,
    pendingCount,
    isSyncingQueue,
    syncPendingReports,
    loadReportIntoAudit,
    deleteReport,
    telecomSites,
    selectSiteForMap,
    setActiveTab,
    showToast
  } = useApp();

  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [photoZoom, setPhotoZoom] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const filteredReports = reports.filter((report) => {
    switch (filterStatus) {
      case 'PENDING':
        return report.syncStatus !== 'SYNCED';
      case 'SYNCED':
        return report.syncStatus === 'SYNCED';
      case 'ACCORDED':
        return report.status === 'ACCORDED';
      case 'REFUSED':
        return report.status === 'REFUSED';
      default:
        return true;
    }
  });

  const handleCopyReport = (report: SiteReport) => {
    const subject = EmailService.generateSubject(report.siteId, report.operator, report.status);
    const body = EmailService.generateBody(
      report.siteId,
      report.siteName,
      report.operator,
      report.status,
      report.refusalReason,
      report.notes,
      report.technicianName,
      report.technicianPhone,
      report.gps,
      report.gsm,
      report.photos.length
    );
    navigator.clipboard.writeText(`SUBJECT: ${subject}\n\n${body}`);
    setCopiedId(report.id);
    showToast('Report copied to clipboard.');
    setTimeout(() => setCopiedId(null), 2500);
  };

  const handleViewOnMap = (report: SiteReport) => {
    const matched = telecomSites.find(s => s.siteId.toLowerCase() === report.siteId.toLowerCase());
    if (matched) {
      selectSiteForMap(matched);
    }
    setActiveTab('MAP');
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 space-y-6 pb-24">
      {/* 1. Offline Queue & Connectivity Banner */}
      <div className={`rounded-2xl p-4 border shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${
        isOnline ? 'bg-emerald-50/80 border-emerald-200' : 'bg-amber-50/80 border-amber-200'
      }`}>
        <div className="flex items-center space-x-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
            isOnline ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
          }`}>
            {isOnline ? <Wifi className="w-5 h-5" /> : <WifiOff className="w-5 h-5" />}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className={`text-xs font-black uppercase ${isOnline ? 'text-emerald-800' : 'text-amber-800'}`}>
                {isOnline ? 'Network Online & Cloud Synchronized' : 'Offline Mode Active'}
              </span>
              {pendingCount > 0 && (
                <span className="bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">
                  {pendingCount} Pending Sync
                </span>
              )}
            </div>
            <p className="text-xs text-slate-600 mt-0.5">
              {pendingCount > 0
                ? `${pendingCount} audit report(s) queued in local storage awaiting cloud dispatch.`
                : 'All audit reports and photo logs are safely synchronized with NOC.'}
            </p>
          </div>
        </div>

        {pendingCount > 0 && (
          <button
            type="button"
            disabled={!isOnline || isSyncingQueue}
            onClick={syncPendingReports}
            className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white text-xs font-bold flex items-center shadow-sm transition-colors shrink-0"
          >
            <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isSyncingQueue ? 'animate-spin' : ''}`} />
            Sync Queue Now
          </button>
        )}
      </div>

      {/* 2. Filter Tabs */}
      <div className="flex space-x-2 overflow-x-auto no-scrollbar pb-1">
        {[
          { key: 'ALL', label: `All Reports (${reports.length})` },
          { key: 'ACCORDED', label: `Accorded (${reports.filter(r => r.status === 'ACCORDED').length})` },
          { key: 'REFUSED', label: `Refused (${reports.filter(r => r.status === 'REFUSED').length})` },
          { key: 'PENDING', label: `Pending Queue (${reports.filter(r => r.syncStatus !== 'SYNCED').length})` },
          { key: 'SYNCED', label: `Synced (${reports.filter(r => r.syncStatus === 'SYNCED').length})` }
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setFilterStatus(tab.key)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all ${
              filterStatus === tab.key
                ? 'bg-telecom-navy text-white shadow-sm'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 3. Reports List */}
      {filteredReports.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200">
          <History className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No Audit Reports Found</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            No logs match the current filter. Completed audits from the field will appear here with full GPS and cell telemetry.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredReports.map((report) => {
            const isAccorded = report.status === 'ACCORDED';
            const isSynced = report.syncStatus === 'SYNCED';
            const dateStr = new Date(report.createdAt).toLocaleString(undefined, {
              dateStyle: 'medium',
              timeStyle: 'short'
            });

            return (
              <div
                key={report.id}
                className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200/90 space-y-4 hover:border-slate-300 transition-all"
              >
                {/* Header Row */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                  <div className="flex items-center space-x-3">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                      isAccorded ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'
                    }`}>
                      {isAccorded ? <CheckCircle2 className="w-6 h-6" /> : <XCircle className="w-6 h-6" />}
                    </div>

                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-mono font-black text-sm text-slate-900">{report.siteId}</span>
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded ${
                          isAccorded ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                        }`}>
                          {isAccorded ? 'ACCESS ACCORDED' : 'ACCESS REFUSED'}
                        </span>
                        <span className="text-[11px] text-slate-400">•</span>
                        <span className="text-xs text-slate-600 font-semibold">{report.operator}</span>
                      </div>
                      <h4 className="text-xs font-bold text-slate-700">{report.siteName}</h4>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2 sm:self-center">
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center ${
                      isSynced ? 'bg-slate-100 text-slate-600' : 'bg-amber-100 text-amber-800'
                    }`}>
                      {isSynced ? (
                        <>
                          <CheckCircle2 className="w-3 h-3 text-emerald-600 mr-1" /> Synced
                        </>
                      ) : (
                        <>
                          <Clock className="w-3 h-3 text-amber-600 mr-1" /> Pending Sync
                        </>
                      )}
                    </span>
                    <span className="text-xs text-slate-400">{dateStr}</span>
                  </div>
                </div>

                {/* Refusal Alert Banner */}
                {!isAccorded && report.refusalReason && (
                  <div className="bg-red-50 border border-red-200 rounded-xl p-3 text-xs text-red-900">
                    <strong className="font-bold">Access Refusal Reason: </strong>
                    <span>{report.refusalReason}</span>
                  </div>
                )}

                {/* Notes */}
                {report.notes && (
                  <p className="text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-100 leading-relaxed">
                    {report.notes}
                  </p>
                )}

                {/* Telemetry Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs font-mono bg-slate-900 text-slate-200 p-3.5 rounded-xl border border-slate-800">
                  <div className="flex items-center space-x-2">
                    <MapPin className="w-4 h-4 text-sky-400 shrink-0" />
                    <span className="text-slate-400">GPS:</span>
                    <span className="text-white truncate">{report.gps.coordinatesString}</span>
                    <span className="text-emerald-400 text-[10px]">±{report.gps.accuracy}m</span>
                  </div>

                  <div className="flex items-center space-x-2">
                    <Signal className="w-4 h-4 text-indigo-400 shrink-0" />
                    <span className="text-slate-400">GSM:</span>
                    <span className="text-indigo-300">{report.gsm.networkType}</span>
                    <span className="text-amber-400">CID {report.gsm.cellId}</span>
                    <span className="text-emerald-400 text-[10px]">{report.gsm.signalDbm}dBm</span>
                  </div>
                </div>

                {/* Attached Photo Thumbnails */}
                {report.photos && report.photos.length > 0 && (
                  <div className="space-y-1.5">
                    <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                      Attached Stamped Evidence ({report.photos.length})
                    </div>
                    <div className="flex space-x-2 overflow-x-auto no-scrollbar py-1">
                      {report.photos.map((photo) => (
                        <div
                          key={photo.id}
                          onClick={() => setPhotoZoom(photo.dataUrl)}
                          className="relative w-24 h-18 rounded-lg overflow-hidden shrink-0 border border-slate-200 cursor-pointer group bg-black"
                        >
                          <img
                            src={photo.dataUrl}
                            alt={photo.tag}
                            className="w-full h-full object-cover group-hover:opacity-80 transition-opacity"
                          />
                          <span className="absolute bottom-0 inset-x-0 bg-black/75 text-[9px] text-white text-center truncate px-1 py-0.5">
                            {photo.tag}
                          </span>
                          <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                            <ZoomIn className="w-4 h-4 text-white" />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Technician Footer & Action Buttons */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between pt-2 border-t border-slate-100 gap-3 text-xs">
                  <div className="flex items-center space-x-2 text-slate-500">
                    <User className="w-3.5 h-3.5" />
                    <span>Reported by: <strong className="text-slate-700">{report.technicianName}</strong> ({report.technicianPhone})</span>
                  </div>

                  <div className="flex items-center space-x-2">
                    <button
                      type="button"
                      onClick={() => handleCopyReport(report)}
                      className="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-slate-700 flex items-center transition-colors text-[11px] font-semibold"
                      title="Copy Email Text"
                    >
                      {copiedId === report.id ? (
                        <>
                          <Check className="w-3 h-3 text-emerald-600 mr-1" /> Copied
                        </>
                      ) : (
                        <>
                          <Copy className="w-3 h-3 text-slate-400 mr-1" /> Copy
                        </>
                      )}
                    </button>

                    <button
                      type="button"
                      onClick={() => handleViewOnMap(report)}
                      className="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-slate-700 flex items-center transition-colors text-[11px] font-semibold"
                      title="View Location on Map"
                    >
                      <Map className="w-3 h-3 text-slate-400 mr-1" /> Map
                    </button>

                    <button
                      type="button"
                      onClick={() => loadReportIntoAudit(report)}
                      className="px-2.5 py-1.5 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 flex items-center transition-colors text-[11px] font-bold"
                      title="Load into Site Audit Form"
                    >
                      <RadioTower className="w-3 h-3 text-blue-600 mr-1" /> Load Audit
                    </button>

                    <button
                      type="button"
                      onClick={() => deleteReport(report.id)}
                      className="p-1.5 rounded-lg text-red-500 hover:bg-red-50 transition-colors"
                      title="Delete Report"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Photo Enlarge Modal */}
      {photoZoom && (
        <div
          onClick={() => setPhotoZoom(null)}
          className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4 cursor-pointer"
        >
          <div className="relative max-w-4xl max-h-[90vh] overflow-hidden rounded-xl">
            <img
              src={photoZoom}
              alt="Watermarked Evidence"
              className="w-full h-full object-contain rounded-xl"
            />
            <button
              onClick={() => setPhotoZoom(null)}
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
