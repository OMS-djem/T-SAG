import React, { useRef, useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { WatermarkService } from '../services/watermark';
import { Camera, X, RefreshCw, Upload, CheckCircle2, ShieldAlert } from 'lucide-react';

const TAG_OPTIONS = [
  'Site Entrance',
  'Telecom Tower / Mast',
  'Equipment Shelter / Cabinet',
  'Access Gate & Lock',
  'Security Guard Post',
  'Power / Generator',
  'Damaged Equipment',
  'Surrounding Area'
];

export const CameraModal: React.FC = () => {
  const { 
    isCameraOpen, 
    closeCamera, 
    activeCameraTag, 
    setActiveCameraTag, 
    addPhoto, 
    siteId, 
    operator, 
    status, 
    gpsData, 
    gsmData 
  } = useApp();

  const videoRef = useRef<HTMLVideoElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [facingMode, setFacingMode] = useState<'environment' | 'user'>('environment');
  const [hasCameraAccess, setHasCameraAccess] = useState<boolean | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    if (!isCameraOpen) {
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
        setStream(null);
      }
      return;
    }

    let isMounted = true;

    const startCamera = async () => {
      try {
        if (stream) {
          stream.getTracks().forEach(track => track.stop());
        }
        const mediaStream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: facingMode },
            width: { ideal: 1920 },
            height: { ideal: 1080 }
          },
          audio: false
        });

        if (isMounted) {
          setStream(mediaStream);
          setHasCameraAccess(true);
          if (videoRef.current) {
            videoRef.current.srcObject = mediaStream;
          }
        }
      } catch (err) {
        console.warn('Camera access denied or device has no camera:', err);
        if (isMounted) {
          setHasCameraAccess(false);
        }
      }
    };

    startCamera();

    return () => {
      isMounted = false;
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
      }
    };
  }, [isCameraOpen, facingMode]);

  const toggleFacingMode = () => {
    setFacingMode(prev => prev === 'environment' ? 'user' : 'environment');
  };

  const capturePhoto = async () => {
    if (!videoRef.current || isProcessing) return;
    setIsProcessing(true);
    try {
      const stampedDataUrl = await WatermarkService.stampImage(videoRef.current, {
        siteId,
        operator,
        status,
        tag: activeCameraTag,
        gps: gpsData,
        gsm: gsmData,
        timestamp: Date.now()
      });

      addPhoto({
        id: `photo_${Date.now()}`,
        dataUrl: stampedDataUrl,
        tag: activeCameraTag,
        timestamp: Date.now(),
        latitude: gpsData.latitude,
        longitude: gpsData.longitude,
        cellId: gsmData.cellId
      });

      closeCamera();
    } catch (err) {
      console.error('Failed to capture and stamp photo:', err);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessing(true);
    const reader = new FileReader();
    reader.onload = async (event) => {
      const dataUrl = event.target?.result as string;
      if (dataUrl) {
        try {
          const stamped = await WatermarkService.stampImage(dataUrl, {
            siteId,
            operator,
            status,
            tag: activeCameraTag,
            gps: gpsData,
            gsm: gsmData,
            timestamp: Date.now()
          });

          addPhoto({
            id: `photo_${Date.now()}`,
            dataUrl: stamped,
            tag: activeCameraTag,
            timestamp: Date.now(),
            latitude: gpsData.latitude,
            longitude: gpsData.longitude,
            cellId: gsmData.cellId
          });

          closeCamera();
        } catch (err) {
          console.error('Error stamping uploaded file:', err);
        } finally {
          setIsProcessing(false);
        }
      }
    };
    reader.readAsDataURL(file);
  };

  if (!isCameraOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col justify-between">
      {/* Top Header */}
      <div className="bg-slate-900/90 text-white px-4 py-3 flex items-center justify-between z-10 border-b border-slate-800">
        <div className="flex items-center space-x-2">
          <Camera className="w-5 h-5 text-sky-400" />
          <div>
            <h2 className="text-sm font-bold text-white">Security Watermark Camera</h2>
            <p className="text-[11px] text-slate-300">Live GPS & GSM Cell Telemetry Overlay</p>
          </div>
        </div>

        <button 
          onClick={closeCamera}
          className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Main Viewfinder Area */}
      <div className="relative flex-1 bg-black flex items-center justify-center overflow-hidden">
        {hasCameraAccess === false ? (
          <div className="p-6 text-center text-white max-w-sm">
            <ShieldAlert className="w-12 h-12 text-amber-400 mx-auto mb-3" />
            <h3 className="text-base font-bold mb-1">Webcam Access Not Available</h3>
            <p className="text-xs text-slate-300 mb-6">
              Camera permissions were blocked or device has no camera. You can upload an image from your device storage to apply the security audit watermark.
            </p>
            <button
              onClick={() => fileInputRef.current?.click()}
              className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm flex items-center justify-center mx-auto shadow-lg"
            >
              <Upload className="w-4 h-4 mr-2" /> Upload Photo File
            </button>
          </div>
        ) : (
          <>
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full h-full object-cover"
            />

            {/* Live HUD Watermark Overlay */}
            <div className="absolute bottom-4 left-4 right-4 bg-slate-950/85 backdrop-blur-sm border-t-2 border-slate-700 p-3 rounded-lg text-white font-mono text-[11px] shadow-2xl pointer-events-none"
              style={{ borderTopColor: status === 'ACCORDED' ? '#10B981' : '#EF4444' }}>
              <div className="text-sky-400 font-bold text-xs tracking-wide">
                MOBISERVE HOLDING • TELECOM SITE AUDIT
              </div>
              <div className="flex flex-wrap gap-x-3 text-slate-200 mt-0.5">
                <span>SITE: <strong className="text-white">{siteId}</strong></span>
                <span>OP: {operator}</span>
                <span>TAG: {activeCameraTag}</span>
              </div>
              <div className="flex items-center space-x-2 mt-0.5">
                <span className="font-bold" style={{ color: status === 'ACCORDED' ? '#34D399' : '#F87171' }}>
                  {status === 'ACCORDED' ? 'STATUS: ACCESS ACCORDED ✓' : 'STATUS: NO ACCESS / REFUSED ✕'}
                </span>
              </div>
              <div className="text-slate-300 text-[10px] mt-0.5 truncate">
                GPS: {gpsData.coordinatesString} (±{gpsData.accuracy}m) | ALT: {gpsData.altitude}m
              </div>
              <div className="text-slate-300 text-[10px] truncate">
                GSM: {gsmData.networkType} | CID: {gsmData.cellId} | LAC: {gsmData.lacTac} | {gsmData.signalDbm} dBm
              </div>
            </div>
          </>
        )}
      </div>

      {/* Tag Selector Bar */}
      <div className="bg-slate-950/95 px-3 py-2 border-t border-slate-800 overflow-x-auto flex space-x-2 no-scrollbar">
        {TAG_OPTIONS.map((tag) => (
          <button
            key={tag}
            type="button"
            onClick={() => setActiveCameraTag(tag)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all flex items-center ${
              activeCameraTag === tag
                ? 'bg-blue-600 text-white shadow-md'
                : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            {activeCameraTag === tag && <CheckCircle2 className="w-3.5 h-3.5 mr-1 text-sky-300" />}
            {tag}
          </button>
        ))}
      </div>

      {/* Bottom Controls Bar */}
      <div className="bg-slate-900 text-white px-6 py-4 flex items-center justify-between z-10">
        {/* Upload Fallback Button */}
        <div>
          <input
            type="file"
            ref={fileInputRef}
            accept="image/*"
            onChange={handleFileUpload}
            className="hidden"
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="p-3 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-200"
            title="Upload from device"
          >
            <Upload className="w-5 h-5" />
          </button>
        </div>

        {/* Shutter Button */}
        <button
          type="button"
          disabled={isProcessing || hasCameraAccess === false}
          onClick={capturePhoto}
          className="w-18 h-18 rounded-full border-4 border-white p-1 flex items-center justify-center bg-blue-600 hover:bg-blue-500 active:scale-95 disabled:opacity-40 transition-all shadow-xl"
        >
          <div className="w-14 h-14 rounded-full bg-white flex items-center justify-center">
            {isProcessing ? (
              <RefreshCw className="w-6 h-6 text-blue-600 animate-spin" />
            ) : (
              <div className="w-12 h-12 rounded-full bg-blue-600" />
            )}
          </div>
        </button>

        {/* Switch Camera facing mode */}
        <button
          type="button"
          onClick={toggleFacingMode}
          className="p-3 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-200"
          title="Switch Camera (Front/Rear)"
        >
          <RefreshCw className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
