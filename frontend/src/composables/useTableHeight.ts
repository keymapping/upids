import { ref, onMounted, onUnmounted, nextTick } from 'vue';

/**
 * 表格高度自适应组合式函数
 * @param containerSelector 容器选择器
 */
export function useTableHeight(containerSelector: string) {
  const tableHeight = ref(400);
  let resizeObserver: ResizeObserver | null = null;

  const updateHeight = () => {
    nextTick(() => {
      const container = document.querySelector(containerSelector);
      if (!container) return;

      const containerHeight = container.clientHeight;

      // 计算筛选区、分页、卡片头等非表格区域的高度
      let usedHeight = 0;

      // 卡片头部
      const cardHeader = container.querySelector('.el-card__header');
      if (cardHeader) usedHeight += cardHeader.getBoundingClientRect().height;

      // 筛选区
      const filterSection = container.querySelector('.filter-section');
      if (filterSection) {
        const style = window.getComputedStyle(filterSection);
        usedHeight += filterSection.getBoundingClientRect().height + parseFloat(style.marginBottom);
      }

      // 分页
      const pagination = container.querySelector('.el-pagination');
      if (pagination) {
        const style = window.getComputedStyle(pagination);
        usedHeight += pagination.getBoundingClientRect().height + parseFloat(style.marginTop);
      }

      // el-card__body 上下内边距
      const cardBody = container.querySelector('.el-card__body');
      if (cardBody) {
        const style = window.getComputedStyle(cardBody);
        usedHeight += parseFloat(style.paddingTop) + parseFloat(style.paddingBottom);
      } else {
        usedHeight += 32; // 默认 16px * 2
      }

      // 额外安全边距
      usedHeight += 8;

      tableHeight.value = Math.max(200, containerHeight - usedHeight);
    });
  };

  onMounted(() => {
    setTimeout(updateHeight, 100);

    resizeObserver = new ResizeObserver(() => {
      updateHeight();
    });

    const container = document.querySelector(containerSelector);
    if (container) {
      resizeObserver.observe(container);
    }

    // 监听窗口大小变化
    window.addEventListener('resize', updateHeight);
  });

  onUnmounted(() => {
    resizeObserver?.disconnect();
    window.removeEventListener('resize', updateHeight);
  });

  return { tableHeight, updateHeight };
}
