import { GpsData, GsmData, SiteAccessStatus } from '../types';

export interface WatermarkOptions {
  siteId: string;
  operator: string;
  status: SiteAccessStatus;
  tag: string;
  gps: GpsData;
  gsm: GsmData;
  timestamp?: number;
}

export const WatermarkService = {
  async stampImage(imageSource: string | HTMLImageElement | HTMLVideoElement, options: WatermarkOptions): Promise<string> {
    return new Promise((resolve, reject) => {
      const processCanvas = (img: HTMLImageElement | HTMLVideoElement) => {
        try {
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');
          if (!ctx) {
            reject(new Error('Canvas 2D context not available'));
            return;
          }

          const width = ('videoWidth' in img && img.videoWidth) ? img.videoWidth : ('naturalWidth' in img && img.naturalWidth) ? img.naturalWidth : (img.width || 1280);
          const height = ('videoHeight' in img && img.videoHeight) ? img.videoHeight : ('naturalHeight' in img && img.naturalHeight) ? img.naturalHeight : (img.height || 720);

          canvas.width = width;
          canvas.height = height;

          // Draw the base photo
          ctx.drawImage(img, 0, 0, width, height);

          // Scaled proportions matching WatermarkHelper.kt
          const baseTextSize = Math.max(16, Math.min(48, Math.round(width * 0.022)));
          const titleTextSize = Math.round(baseTextSize * 1.25);
          const padding = Math.max(14, Math.min(36, Math.round(width * 0.028)));

          const dateObj = new Date(options.timestamp || Date.now());
          const timeString = dateObj.toISOString().replace('T', ' ').substring(0, 19);

          const lines = [
            "MOBISERVE HOLDING • TELECOM SITE ACCESS AUDIT",
            `SITE ID: ${options.siteId || 'SITE-UNKNOWN'}  |  OPERATOR: ${options.operator || 'N/A'}  |  TAG: ${options.tag}`,
            `STATUS: ${options.status === 'ACCORDED' ? 'ACCESS ACCORDED ✓' : 'NO ACCESS / REFUSED ✕'}`,
            `GPS: ${options.gps.coordinatesString} (±${options.gps.accuracy}m)  |  ALT: ${options.gps.altitude}m`,
            `GSM: ${options.gsm.networkType}  |  MCC-MNC: ${options.gsm.mcc}-${options.gsm.mnc}  |  LAC/TAC: ${options.gsm.lacTac}  |  CID: ${options.gsm.cellId}  |  ${options.gsm.signalDbm} dBm`,
            `TIMESTAMP: ${timeString}`
          ];

          const lineSpacing = baseTextSize * 1.45;
          const bannerHeight = (lines.length * lineSpacing) + (padding * 2.2);

          // Draw translucent dark backdrop on bottom of the image
          ctx.fillStyle = 'rgba(10, 20, 35, 0.88)';
          ctx.fillRect(0, height - bannerHeight, width, bannerHeight);

          // Draw top accent line on banner (Green for Accorded, Red for Refused)
          ctx.strokeStyle = options.status === 'ACCORDED' ? '#10B981' : '#EF4444';
          ctx.lineWidth = Math.max(4, Math.round(width * 0.006));
          ctx.beginPath();
          ctx.moveTo(0, height - bannerHeight);
          ctx.lineTo(width, height - bannerHeight);
          ctx.stroke();

          // Render text lines
          let currentY = height - bannerHeight + padding + titleTextSize;

          lines.forEach((line, index) => {
            if (index === 0) {
              // Brand Header
              ctx.font = `bold ${titleTextSize}px "Segoe UI", Arial, sans-serif`;
              ctx.fillStyle = '#38BDF8'; // Light Cyan
            } else if (index === 2) {
              // Status Line
              ctx.font = `bold ${Math.round(baseTextSize * 1.1)}px "Segoe UI", Arial, sans-serif`;
              ctx.fillStyle = options.status === 'ACCORDED' ? '#34D399' : '#F87171';
            } else {
              // Telemetry & Details Monospace
              ctx.font = `normal ${baseTextSize}px "Courier New", Courier, monospace`;
              ctx.fillStyle = '#FFFFFF';
            }

            ctx.fillText(line, padding, currentY);
            currentY += lineSpacing;
          });

          const stampedDataUrl = canvas.toDataURL('image/jpeg', 0.9);
          resolve(stampedDataUrl);
        } catch (err) {
          reject(err);
        }
      };

      if (typeof imageSource === 'string') {
        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.onload = () => processCanvas(img);
        img.onerror = (e) => reject(e);
        img.src = imageSource;
      } else {
        processCanvas(imageSource);
      }
    });
  }
};
