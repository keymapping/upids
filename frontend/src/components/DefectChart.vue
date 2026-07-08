<template>
  <div class="defect-chart-container">
    <div ref="chartContainer" class="chart"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import * as echarts from 'echarts';

interface Props {
  title?: string;
  type?: 'pie' | 'bar' | 'line' | 'ring';
  data?: Array<{ name: string; value: number }>;
  height?: string;
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  type: 'pie',
  data: () => [],
  height: '300px',
});

const chartContainer = ref<HTMLElement>();
let chart: echarts.ECharts | null = null;

const colorPalette = [
  '#409eff',
  '#67c23a',
  '#e6a23c',
  '#f56c6c',
  '#909399',
  '#b37feb',
  '#36cfc9',
  '#ff85c0',
];

const buildOption = (): echarts.EChartsOption => {
  const names = props.data.map((item) => item.name);
  const values = props.data.map((item) => item.value);

  switch (props.type) {
    case 'pie':
      return {
        title: {
          text: props.title,
          left: 'center',
          textStyle: { fontSize: 14 },
        },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left', top: 'middle' },
        color: colorPalette,
        series: [
          {
            type: 'pie',
            radius: ['0%', '55%'],
            center: ['55%', '55%'],
            data: props.data,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0,0,0,0.5)',
              },
            },
            label: { formatter: '{b}\n{d}%' },
          },
        ],
      };

    case 'ring':
      return {
        title: {
          text: props.title,
          left: 'center',
          textStyle: { fontSize: 14 },
        },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left', top: 'middle' },
        color: colorPalette,
        series: [
          {
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['55%', '55%'],
            avoidLabelOverlap: false,
            label: { show: false },
            emphasis: {
              label: { show: true, fontSize: 14, fontWeight: 'bold' },
            },
            labelLine: { show: false },
            data: props.data,
          },
        ],
      };

    case 'bar':
      return {
        title: {
          text: props.title,
          left: 'center',
          textStyle: { fontSize: 14 },
        },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: {
          type: 'category',
          data: names,
          axisLabel: { rotate: names.length > 6 ? 30 : 0 },
        },
        yAxis: { type: 'value' },
        color: colorPalette,
        series: [
          {
            type: 'bar',
            data: values,
            barMaxWidth: 40,
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
            },
          },
        ],
      };

    case 'line':
      return {
        title: {
          text: props.title,
          left: 'center',
          textStyle: { fontSize: 14 },
        },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: { type: 'category', data: names, boundaryGap: false },
        yAxis: { type: 'value' },
        color: colorPalette,
        series: [
          {
            type: 'line',
            data: values,
            smooth: true,
            areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
            itemStyle: { color: '#409eff' },
          },
        ],
      };

    default:
      return {};
  }
};

const updateChart = () => {
  if (!chart) return;
  chart.setOption(buildOption(), true);
};

watch(() => props.data, updateChart, { deep: true });
watch(() => props.type, updateChart);
watch(() => props.title, updateChart);

const handleResize = () => {
  if (chart) chart.resize();
};

onMounted(() => {
  if (chartContainer.value) {
    chart = echarts.init(chartContainer.value);
    updateChart();
  }
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  if (chart) {
    chart.dispose();
    chart = null;
  }
  window.removeEventListener('resize', handleResize);
});

defineExpose({
  chart,
  resize: handleResize,
});
</script>

<style scoped>
.defect-chart-container {
  width: 100%;
  height: v-bind(height);
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
