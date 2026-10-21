/* Bonto Bridge – dodawany automatycznie do index.html. Użycie: window.Bonto */
(function () {
  var C = window.Capacitor, P = (C && C.Plugins) || {};
  var isApp = !!(C && C.isNativePlatform && C.isNativePlatform());
  var sleep = function (ms) { return new Promise(function (r) { setTimeout(r, ms); }); };

  var Bonto = {
    isApp: isApp,

    /** vibrate(500) lub vibrate([200,100,200]) – nieparzyste elementy to pauzy */
    vibrate: async function (pattern) {
      var p = Array.isArray(pattern) ? pattern : [pattern || 300];
      if (!isApp && navigator.vibrate) return navigator.vibrate(p), true;
      if (!isApp || !P.Haptics) return false;
      for (var i = 0; i < p.length; i++) {
        if (i % 2 === 0) await P.Haptics.vibrate({ duration: p[i] }), await sleep(p[i]);
        else await sleep(p[i]);
      }
      return true;
    },

    requestNotifications: async function () {
      if (isApp && P.LocalNotifications) {
        var s = await P.LocalNotifications.checkPermissions();
        if (s.display !== 'granted') s = await P.LocalNotifications.requestPermissions();
        return s.display === 'granted';
      }
      if (!('Notification' in window)) return false;
      return (await Notification.requestPermission()) === 'granted';
    },

    /** notify({title, body, delay: sekundy, at: Date|ms, id}) → zwraca id */
    notify: async function (o) {
      o = o || {};
      var id = o.id || Math.floor(Math.random() * 2147483000) + 1;
      var at = o.at ? new Date(o.at) : o.delay ? new Date(Date.now() + o.delay * 1000) : null;
      if (!(await Bonto.requestNotifications())) return false;
      if (isApp && P.LocalNotifications) {
        var n = { id: id, title: o.title || '', body: o.body || '' };
        if (at) n.schedule = { at: at, allowWhileIdle: true };
        await P.LocalNotifications.schedule({ notifications: [n] });
      } else {
        setTimeout(function () { new Notification(o.title || '', { body: o.body || '' }); }, at ? at - Date.now() : 0);
      }
      return id;
    },

    cancelNotification: async function (id) {
      var ids = Array.isArray(id) ? id : [id];
      if (isApp && P.LocalNotifications) await P.LocalNotifications.cancel({ notifications: ids.map(function (i) { return { id: i }; }) });
    },

    /** remind({title, body, weekday|weekdays: 1-7 (pon=1), hour, minute}) – cyklicznie */
    remind: async function (o) {
      if (!isApp || !P.LocalNotifications || !(await Bonto.requestNotifications())) return false;
      var days = o.weekdays || (o.weekday ? [o.weekday] : [null]);
      var base = Math.floor(Math.random() * 2000000000) + 1, ids = [], list = [];
      days.forEach(function (d, i) {
        var on = { hour: o.hour || 0, minute: o.minute || 0 };
        if (d) on.weekday = (d % 7) + 1; // Capacitor: 1 = niedziela
        ids.push(base + i);
        list.push({ id: base + i, title: o.title || '', body: o.body || '', schedule: { on: on, allowWhileIdle: true } });
      });
      await P.LocalNotifications.schedule({ notifications: list });
      return ids;
    },

    widget: {
      /** setDays({'2026-10-22': {title, body}, ...}) – widżet sam wybiera dzisiejszy dzień */
      setDays: async function (map) {
        if (!isApp || !P.BontoWidget) return false;
        await P.BontoWidget.setWidgetDays({ days: map });
        return true;
      },
      set: async function (o) {
        if (!isApp || !P.BontoWidget) return false;
        await P.BontoWidget.setWidget({ title: o.title || '', body: (o.lines || []).join('\n') });
        return true;
      }
    },

    share: async function (o) {
      if (isApp && P.Share) return P.Share.share(o), true;
      if (navigator.share) return navigator.share(o), true;
      return false;
    },

    /** http(url, {method, headers, body, timeout}) – natywne zapytanie bez CORS; zwraca obiekt jak fetch (ok, status, text()) */
    http: async function (url, o) {
      o = o || {};
      if (!isApp || !P.CapacitorHttp) return fetch(url, o);
      var H = o.headers || {}, data = o.body;
      if (typeof data === 'string' && /json/i.test(H['Content-Type'] || H['content-type'] || '')) { try { data = JSON.parse(data); } catch (e) {} }
      var r = await P.CapacitorHttp.request({ url: url, method: o.method || 'GET', headers: H, data: data, responseType: 'text', connectTimeout: o.timeout || 15000, readTimeout: o.timeout || 15000 });
      var t = typeof r.data === 'string' ? r.data : JSON.stringify(r.data);
      return { ok: r.status >= 200 && r.status < 300, status: r.status, text: async function () { return t; } };
    },

    /** Sprawdzanie zastępstw Edupage w tle (co ok. 15 min) + powiadomienia o odwołanych lekcjach */
    edupage: {
      watch: async function (o) {
        if (!isApp || !P.BontoWidget) return false;
        await P.BontoWidget.watchEdupage(o);
        return true;
      },
      stop: async function () { if (isApp && P.BontoWidget) await P.BontoWidget.stopEdupage(); }
    },

    clipboard: {
      write: async function (t) {
        if (isApp && P.Clipboard) await P.Clipboard.write({ string: t });
        else await navigator.clipboard.writeText(t);
        return true;
      },
      read: async function () {
        if (isApp && P.Clipboard) return (await P.Clipboard.read()).value;
        return navigator.clipboard.readText();
      }
    },

    /** location() → {lat, lng, accuracy} */
    location: async function () {
      var c;
      if (isApp && P.Geolocation) c = (await P.Geolocation.getCurrentPosition({ enableHighAccuracy: true })).coords;
      else c = (await new Promise(function (ok, no) { navigator.geolocation.getCurrentPosition(ok, no); })).coords;
      return { lat: c.latitude, lng: c.longitude, accuracy: c.accuracy };
    },

    /** photo('camera' | 'gallery' | 'prompt') → data URL (null jeśli anulowano / poza aplikacją) */
    photo: async function (source) {
      if (!isApp || !P.Camera) return null;
      var src = { camera: 'CAMERA', gallery: 'PHOTOS' }[source] || 'PROMPT';
      try { return (await P.Camera.getPhoto({ quality: 80, resultType: 'dataUrl', source: src })).dataUrl; }
      catch (e) { return null; }
    },

    exitApp: function () { if (isApp && P.App) P.App.exitApp(); }
  };
  window.Bonto = Bonto;
})();
