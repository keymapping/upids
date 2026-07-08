<template>
  <div class="gis-map-container">
    <div ref="mapContainer" class="map"></div>
    <div v-if="!mapLoaded && !mapError" class="map-loading">
      <div class="loading-spinner"></div>
      <span>地图加载中...</span>
    </div>
    <div v-if="mapError" class="map-error">
      <span>地图加载失败，请检查网络连接</span>
      <el-button type="primary" size="small" @click="reloadMap">重新加载</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { GeoJSONFeatureCollection, GeoJSONFeature } from '@/types/api';

interface Props {
  center?: [number, number];
  zoom?: number;
  pipelines?: GeoJSONFeatureCollection | null;
  defects?: GeoJSONFeatureCollection | null;
}

const props = withDefaults(defineProps<Props>(), {
  center: () => [39.9, 116.4],
  zoom: 12,
  pipelines: null,
  defects: null,
});

const emit = defineEmits<{
  'click-pipeline': [feature: GeoJSONFeature];
  'click-defect': [feature: GeoJSONFeature];
  'map-ready': [];
}>();

const mapContainer = ref<HTMLElement>();
const mapLoaded = ref(false);
const mapError = ref(false);
let map: L.Map | null = null;
let pipelineLayer: L.GeoJSON | null = null;
let defectLayer: L.GeoJSON | null = null;
let pipelineLayerGroup: L.LayerGroup | null = null;
let defectLayerGroup: L.LayerGroup | null = null;

// 底图服务列表（按优先级排序，国内服务优先）
const tileLayers = [
  {
    url: 'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    options: {
      attribution: '&copy; 高德地图',
      minZoom: 3,
      maxZoom: 18,
      subdomains: ['1', '2', '3', '4'],
    },
  },
  {
    url: 'https://t{s}.tianditu.gov.cn/vec_w/wmts?service=WMTS&request=GetTile&version=1.0.0&layer=vec&style=default&tilematrixset=w&TileMatrix={z}&TileRow={y}&TileCol={x}&format=tiles&tk=demo',
    options: {
      attribution: '&copy; 天地图',
      minZoom: 3,
      maxZoom: 18,
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
    },
  },
  {
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    options: {
      attribution: '&copy; OpenStreetMap contributors',
      minZoom: 3,
      maxZoom: 19,
    },
  },
  {
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
    options: {
      attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
      minZoom: 3,
      maxZoom: 18,
      subdomains: 'abcd',
    },
  },
];

// 缺陷等级对应颜色
const severityColor = (level: number): string => {
  if (level >= 4) return '#f56c6c';
  if (level >= 2) return '#e6a23c';
  return '#f9e56b';
};

// 缺陷类型中文
const defectTypeLabel = (type: string): string => {
  const map: Record<string, string> = {
    crack: '裂缝',
    corrosion: '腐蚀',
    fracture: '断裂',
    none: '无缺陷',
  };
  return map[type] || type;
};

// 尝试加载底图，失败则切换到下一个
const loadTileLayer = (index: number = 0): void => {
  if (!map || index >= tileLayers.length) {
    mapError.value = true;
    return;
  }

  const layerConfig = tileLayers[index];
  const layer = L.tileLayer(layerConfig.url, {
    ...layerConfig.options,
    crossOrigin: true,
    keepBuffer: 2,
  });

  let errorCount = 0;
  const maxErrors = 3;

  layer.on('tileerror', () => {
    errorCount++;
    if (errorCount >= maxErrors) {
      layer.remove();
      loadTileLayer(index + 1);
    }
  });

  layer.on('load', () => {
    mapLoaded.value = true;
  });

  layer.addTo(map);
};

// 初始化地图
const initMap = async () => {
  if (!mapContainer.value) return;

  await nextTick();

  map = L.map(mapContainer.value, {
    center: props.center,
    zoom: props.zoom,
    zoomControl: false,
    minZoom: 3,
    maxZoom: 18,
    worldCopyJump: true,
    preferCanvas: true,
    renderer: L.canvas({ padding: 0.5 }),
  });

  L.control.zoom({ position: 'topright' }).addTo(map);

  pipelineLayerGroup = L.layerGroup().addTo(map);
  defectLayerGroup = L.layerGroup().addTo(map);

  loadTileLayer(0);

  setTimeout(() => {
    if (!mapLoaded.value && !mapError.value) {
      mapLoaded.value = true;
    }
  }, 3000);

  emit('map-ready');
};

// 渲染管线图层
const renderPipelines = () => {
  if (!map || !pipelineLayerGroup) return;
  pipelineLayerGroup.clearLayers();

  if (!props.pipelines || !props.pipelines.features?.length) return;

  const currentZoom = map.getZoom();
  const weight = currentZoom >= 15 ? 4 : currentZoom >= 12 ? 3 : 2;
  const opacity = currentZoom >= 15 ? 0.8 : currentZoom >= 12 ? 0.6 : 0.4;

  pipelineLayer = L.geoJSON(props.pipelines as any, {
    style: () => ({
      color: '#409eff',
      weight,
      opacity,
      lineCap: 'round',
      lineJoin: 'round',
    }),
    onEachFeature: (feature, layer) => {
      const p = feature.properties;
      const popupContent = `
        <div style="min-width:180px">
          <h4 style="margin:0 0 8px;font-size:14px">${p.pipelineName || p.pipelineId}</h4>
          <p style="margin:4px 0;font-size:12px"><b>编号：</b>${p.pipelineId}</p>
          <p style="margin:4px 0;font-size:12px"><b>材质：</b>${p.materialType || '-'}</p>
          <p style="margin:4px 0;font-size:12px"><b>管径：</b>${p.diameter ? p.diameter + 'mm' : '-'}</p>
          <p style="margin:4px 0;font-size:12px"><b>缺陷数：</b>${p.defectCount ?? 0}</p>
        </div>
      `;
      layer.bindPopup(popupContent, { maxWidth: 240 });
      layer.on('click', () => {
        emit('click-pipeline', feature as unknown as GeoJSONFeature);
      });
    },
  });
  pipelineLayerGroup.addLayer(pipelineLayer);
};

// 渲染缺陷图层
const renderDefects = () => {
  if (!map || !defectLayerGroup) return;
  defectLayerGroup.clearLayers();

  if (!props.defects || !props.defects.features?.length) return;

  const currentZoom = map.getZoom();
  const radius = currentZoom >= 15 ? 10 : currentZoom >= 12 ? 8 : currentZoom >= 10 ? 6 : 4;

  defectLayer = L.geoJSON(props.defects as any, {
    pointToLayer: (_feature, latlng) => {
      const level = _feature.properties?.severityLevel || 1;
      const color = severityColor(level);
      return L.circleMarker(latlng, {
        radius,
        fillColor: color,
        color: '#fff',
        weight: currentZoom >= 12 ? 2 : 1,
        opacity: 1,
        fillOpacity: 0.85,
      });
    },
    onEachFeature: (feature, layer) => {
      const p = feature.properties;
      const popupContent = `
        <div style="min-width:180px">
          <h4 style="margin:0 0 8px;font-size:14px">${defectTypeLabel(p.defectType)} 缺陷</h4>
          <p style="margin:4px 0;font-size:12px"><b>缺陷ID：</b>${p.defectId}</p>
          <p style="margin:4px 0;font-size:12px"><b>管线：</b>${p.pipelineId}</p>
          <p style="margin:4px 0;font-size:12px"><b>类型：</b>${defectTypeLabel(p.defectType)}</p>
          <p style="margin:4px 0;font-size:12px"><b>等级：</b>${p.severityLevel}/5</p>
        </div>
      `;
      layer.bindPopup(popupContent, { maxWidth: 240 });
      layer.on('click', () => {
        emit('click-defect', feature as unknown as GeoJSONFeature);
      });
    },
  });
  defectLayerGroup.addLayer(defectLayer);
};

// 获取当前地图边界
const getBounds = () => {
  if (!map) return null;
  const bounds = map.getBounds();
  return {
    minLng: bounds.getWest(),
    minLat: bounds.getSouth(),
    maxLng: bounds.getEast(),
    maxLat: bounds.getNorth(),
  };
};

watch(() => props.pipelines, renderPipelines, { deep: true });
watch(() => props.defects, renderDefects, { deep: true });

onMounted(() => {
  initMap();
});

onBeforeUnmount(() => {
  if (map) {
    map.remove();
    map = null;
  }
});

const reloadMap = () => {
  mapError.value = false;
  mapLoaded.value = false;
  if (map) {
    map.remove();
    map = null;
  }
  initMap();
};

defineExpose({
  map,
  getBounds,
  fitBounds: (bounds: L.LatLngBoundsExpression) => {
    if (map) map.fitBounds(bounds);
  },
});
</script>

<style scoped>
.gis-map-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.map {
  width: 100%;
  height: 100%;
}

.map-loading,
.map-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.9);
  padding: 24px 32px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  z-index: 1000;
}

.map-loading span,
.map-error span {
  font-size: 14px;
  color: #606266;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e4e7ed;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.map-error {
  gap: 16px;
}
</style>
