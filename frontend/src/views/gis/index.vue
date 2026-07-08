<template>
  <div class="gis-page">
    <!-- 左侧控制面板 -->
    <div class="control-panel" :class="{ collapsed: panelCollapsed }">
      <el-card shadow="never" class="panel-card" v-show="!panelCollapsed">
        <template #header>
          <span class="panel-title">图层控制</span>
        </template>

        <div class="layer-toggle">
          <el-switch
            v-model="showPipelineLayer"
            active-text="管线图层"
            @change="onLayerChange"
          />
        </div>
        <div class="layer-toggle">
          <el-switch
            v-model="showDefectLayer"
            active-text="缺陷图层"
            @change="onLayerChange"
          />
        </div>

        <el-divider />

        <div class="panel-title" style="margin-bottom: 12px">缺陷筛选</div>

        <el-form label-position="top" size="small">
          <el-form-item label="缺陷类型">
            <el-select
              v-model="filterDefectType"
              placeholder="全部类型"
              clearable
              @change="onFilterChange"
            >
              <el-option label="裂缝" value="crack" />
              <el-option label="腐蚀" value="corrosion" />
              <el-option label="断裂" value="fracture" />
            </el-select>
          </el-form-item>

          <el-form-item label="最低严重等级">
            <el-slider
              v-model="filterMinSeverity"
              :min="1"
              :max="5"
              :step="1"
              show-stops
              show-tooltip
              @change="onFilterChange"
            />
          </el-form-item>
        </el-form>

        <el-button
          type="primary"
          style="width: 100%; margin-top: 8px"
          @click="refreshData"
        >
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>

        <el-divider />

        <div class="legend">
          <div class="panel-title" style="margin-bottom: 8px">图例</div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #409eff"></span> 管线
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #f56c6c"></span>
            高等级缺陷 (4-5)
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #e6a23c"></span>
            中等级缺陷 (2-3)
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #f9e56b"></span>
            低等级缺陷 (1)
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图层控制切换按钮 -->
    <el-button
      class="panel-toggle-btn"
      circle
      size="small"
      @click="panelCollapsed = !panelCollapsed"
    >
      <el-icon>
        <Expand v-if="panelCollapsed" />
        <Fold v-else />
      </el-icon>
    </el-button>

    <!-- 地图区域 -->
    <div class="map-area">
      <GisMap
        ref="gisMapRef"
        :center="mapCenter"
        :zoom="mapZoom"
        :pipelines="pipelineData"
        :defects="defectData"
        @click-pipeline="onPipelineClick"
        @click-defect="onDefectClick"
        @map-ready="onMapReady"
      />
    </div>

    <!-- 管线详情弹窗 -->
    <el-dialog v-model="pipelineDialogVisible" title="管线详情" width="480px">
      <template v-if="selectedPipeline">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="编号">{{
            selectedPipeline.pipelineId
          }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{
            selectedPipeline.pipelineName
          }}</el-descriptions-item>
          <el-descriptions-item label="材质">{{
            selectedPipeline.materialType || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="管径">{{
            selectedPipeline.diameter ? selectedPipeline.diameter + 'mm' : '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{
            selectedPipeline.regionCode || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedPipeline.status === 1 ? 'success' : 'danger'" size="small">
              {{ selectedPipeline.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="缺陷数" :span="2">
            <span :class="{ 'text-danger font-bold': (selectedPipeline.defectCount || 0) > 0 }">
              {{ selectedPipeline.defectCount ?? 0 }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider />
        <div class="dialog-actions">
          <el-button type="primary" @click="goToPipelineDetail(selectedPipeline.pipelineId)">
            <el-icon><ArrowRight /></el-icon>
            查看完整详情
          </el-button>
          <el-button @click="goToInspection(selectedPipeline.pipelineId)">
            <el-icon><Search /></el-icon>
            查看巡检记录
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 缺陷详情弹窗 -->
    <el-dialog v-model="defectDialogVisible" title="缺陷详情" width="420px">
      <template v-if="selectedDefect">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="缺陷ID">{{
            selectedDefect.defectId
          }}</el-descriptions-item>
          <el-descriptions-item label="所属管线">{{
            selectedDefect.pipelineId
          }}</el-descriptions-item>
          <el-descriptions-item label="缺陷类型">
            <el-tag :type="defectTagType(selectedDefect.defectType)">{{
              defectTypeLabel(selectedDefect.defectType)
            }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="严重等级">
            <el-rate v-model="selectedDefect.severityLevel" disabled :max="5" />
          </el-descriptions-item>
          <el-descriptions-item label="置信度"
            >{{
              (selectedDefect.confidenceScore * 100).toFixed(1)
            }}%</el-descriptions-item
          >
          <el-descriptions-item label="检测来源">{{
            selectedDefect.source
          }}</el-descriptions-item>
          <el-descriptions-item label="检测时间">{{
            selectedDefect.detectedAt
          }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh, Expand, Fold, ArrowRight, Search } from '@element-plus/icons-vue';
import GisMap from '@/components/GisMap.vue';
import { getPipelinesGeoJSON } from '@/api/pipeline';
import { getDefectsGeoJSON } from '@/api/defect';
import type {
  GeoJSONFeatureCollection,
  GeoJSONFeature,
  PipelineVO,
  DefectVO,
} from '@/types/api';
import './style.css';

const router = useRouter();
const gisMapRef = ref<InstanceType<typeof GisMap>>();
const panelCollapsed = ref(false);

const mapCenter = ref<[number, number]>([39.9, 116.4]);
const mapZoom = ref(12);

const showPipelineLayer = ref(true);
const showDefectLayer = ref(true);
const filterDefectType = ref('');
const filterMinSeverity = ref(1);

const pipelineData = ref<GeoJSONFeatureCollection | null>(null);
const defectData = ref<GeoJSONFeatureCollection | null>(null);

const pipelineDialogVisible = ref(false);
const defectDialogVisible = ref(false);
const selectedPipeline = ref<PipelineVO | null>(null);
const selectedDefect = ref<DefectVO | null>(null);

const isLoading = ref(false);
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const defectTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    crack: '裂缝',
    corrosion: '腐蚀',
    fracture: '断裂',
    none: '无缺陷',
  };
  return map[type] || type;
};

const defectTagType = (type: string) => {
  const map: Record<string, string> = {
    crack: 'danger',
    corrosion: 'warning',
    fracture: 'danger',
  };
  return (map[type] || 'info') as any;
};

const debounceLoadData = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  debounceTimer = setTimeout(() => {
    loadData();
  }, 300);
};

const loadData = async () => {
  if (isLoading.value) return;
  
  const bounds = gisMapRef.value?.getBounds();
  if (!bounds) return;

  isLoading.value = true;

  try {
    if (showPipelineLayer.value) {
      const res = await getPipelinesGeoJSON(bounds);
      pipelineData.value = res.data;
    } else {
      pipelineData.value = null;
    }
  } catch {}

  try {
    if (showDefectLayer.value) {
      const params: any = { ...bounds };
      if (filterDefectType.value) params.defectType = filterDefectType.value;
      if (filterMinSeverity.value > 1)
        params.minSeverity = filterMinSeverity.value;
      const res = await getDefectsGeoJSON(params);
      defectData.value = res.data;
    } else {
      defectData.value = null;
    }
  } catch {}

  isLoading.value = false;
};

const onLayerChange = () => {
  debounceLoadData();
};

const onFilterChange = () => {
  debounceLoadData();
};

const refreshData = () => {
  loadData();
  ElMessage.success('数据已刷新');
};

const onPipelineClick = (feature: GeoJSONFeature) => {
  const p = feature.properties as any;
  selectedPipeline.value = {
    pipelineId: p.pipelineId,
    pipelineName: p.pipelineName,
    materialType: p.materialType,
    diameter: p.diameter,
    defectCount: p.defectCount,
  } as PipelineVO;
  pipelineDialogVisible.value = true;
};

const onDefectClick = (feature: GeoJSONFeature) => {
  const p = feature.properties as any;
  selectedDefect.value = {
    defectId: p.defectId,
    pipelineId: p.pipelineId,
    defectType: p.defectType,
    severityLevel: p.severityLevel,
    confidenceScore: p.confidenceScore || 0,
    source: p.source || '-',
    detectedAt: p.detectedAt || '-',
  } as DefectVO;
  defectDialogVisible.value = true;
};

const onMapReady = () => {
  loadData();
};

const goToPipelineDetail = (pipelineId: string) => {
  router.push(`/pipeline/detail/${pipelineId}`);
  pipelineDialogVisible.value = false;
};

const goToInspection = (pipelineId: string) => {
  router.push(`/inspection?keyword=${pipelineId}`);
  pipelineDialogVisible.value = false;
};

onMounted(() => {});

onBeforeUnmount(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
    debounceTimer = null;
  }
});
</script>
