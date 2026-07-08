"""
合成训练数据生成器

生成模拟管道缺陷图像用于CNN模型训练。
由于没有真实缺陷图像数据集，使用程序化方式生成具有缺陷特征的合成图像。

图像类别:
- none: 正常管道表面（均匀纹理）
- crack: 裂缝缺陷（线状纹理）
- corrosion: 腐蚀缺陷（斑点状纹理）
- fracture: 断裂缺陷（大面积破损纹理）
"""

import os
import random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter


# 缺陷类别配置
DEFECT_CLASSES = {
    'none': {'color_range': (100, 180), 'description': '正常管道'},
    'crack': {'color_range': (60, 120), 'description': '裂缝缺陷'},
    'corrosion': {'color_range': (80, 140), 'description': '腐蚀缺陷'},
    'fracture': {'color_range': (40, 100), 'description': '断裂缺陷'}
}

IMAGE_SIZE = 128


def generate_none_image():
    """生成正常管道表面图像（均匀纹理）"""
    img = Image.new('RGB', (IMAGE_SIZE, IMAGE_SIZE))
    draw = ImageDraw.Draw(img)

    # 基础管道颜色（灰蓝色调）
    base_r = random.randint(120, 160)
    base_g = random.randint(130, 170)
    base_b = random.randint(140, 180)

    # 填充基础色
    draw.rectangle([0, 0, IMAGE_SIZE, IMAGE_SIZE],
                   fill=(base_r, base_g, base_b))

    # 添加轻微噪声纹理
    pixels = np.array(img)
    noise = np.random.normal(0, 8, pixels.shape).astype(np.int16)
    pixels = np.clip(pixels.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    img = Image.fromarray(pixels)

    # 添加轻微模糊使纹理更自然
    img = img.filter(ImageFilter.GaussianBlur(radius=0.5))

    return img


def generate_crack_image():
    """生成裂缝缺陷图像（线状纹理）"""
    img = Image.new('RGB', (IMAGE_SIZE, IMAGE_SIZE))
    draw = ImageDraw.Draw(img)

    # 管道基础色
    base_r = random.randint(110, 150)
    base_g = random.randint(120, 160)
    base_b = random.randint(130, 170)
    draw.rectangle([0, 0, IMAGE_SIZE, IMAGE_SIZE],
                   fill=(base_r, base_g, base_b))

    # 绘制裂缝（黑色或深灰色线段）
    crack_color = (random.randint(20, 50), random.randint(20, 50), random.randint(20, 50))
    num_cracks = random.randint(1, 3)

    for _ in range(num_cracks):
        # 随机起点
        x1 = random.randint(0, IMAGE_SIZE)
        y1 = random.randint(0, IMAGE_SIZE)

        # 生成折线裂缝
        points = [(x1, y1)]
        num_segments = random.randint(3, 6)

        for _ in range(num_segments):
            dx = random.randint(-30, 30)
            dy = random.randint(-30, 30)
            new_x = max(0, min(IMAGE_SIZE, points[-1][0] + dx))
            new_y = max(0, min(IMAGE_SIZE, points[-1][1] + dy))
            points.append((new_x, new_y))

        # 绘制裂缝线
        crack_width = random.randint(1, 3)
        draw.line(points, fill=crack_color, width=crack_width)

    # 添加轻微噪声
    pixels = np.array(img)
    noise = np.random.normal(0, 5, pixels.shape).astype(np.int16)
    pixels = np.clip(pixels.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    img = Image.fromarray(pixels)

    return img


def generate_corrosion_image():
    """生成腐蚀缺陷图像（斑点状纹理）"""
    img = Image.new('RGB', (IMAGE_SIZE, IMAGE_SIZE))
    draw = ImageDraw.Draw(img)

    # 管道基础色（略带锈色）
    base_r = random.randint(130, 170)
    base_g = random.randint(110, 150)
    base_b = random.randint(100, 140)
    draw.rectangle([0, 0, IMAGE_SIZE, IMAGE_SIZE],
                   fill=(base_r, base_g, base_b))

    # 绘制腐蚀斑点（深色圆形区域）
    num_spots = random.randint(5, 15)
    for _ in range(num_spots):
        x = random.randint(0, IMAGE_SIZE)
        y = random.randint(0, IMAGE_SIZE)
        radius = random.randint(5, 20)

        # 锈蚀颜色（深棕色）
        spot_color = (
            random.randint(60, 100),
            random.randint(40, 80),
            random.randint(30, 60)
        )
        draw.ellipse([x - radius, y - radius, x + radius, y + radius],
                     fill=spot_color)

    # 添加轻微模糊
    img = img.filter(ImageFilter.GaussianBlur(radius=1.0))

    # 添加噪声
    pixels = np.array(img)
    noise = np.random.normal(0, 10, pixels.shape).astype(np.int16)
    pixels = np.clip(pixels.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    img = Image.fromarray(pixels)

    return img


def generate_fracture_image():
    """生成断裂缺陷图像（大面积破损纹理）"""
    img = Image.new('RGB', (IMAGE_SIZE, IMAGE_SIZE))
    draw = ImageDraw.Draw(img)

    # 管道基础色
    base_r = random.randint(100, 150)
    base_g = random.randint(110, 160)
    base_b = random.randint(120, 170)
    draw.rectangle([0, 0, IMAGE_SIZE, IMAGE_SIZE],
                   fill=(base_r, base_g, base_b))

    # 绘制断裂区域（深色不规则多边形）
    fracture_color = (random.randint(30, 60), random.randint(30, 60), random.randint(30, 60))

    # 生成不规则断裂区域
    num_fractures = random.randint(1, 3)
    for _ in range(num_fractures):
        center_x = random.randint(20, IMAGE_SIZE - 20)
        center_y = random.randint(20, IMAGE_SIZE - 20)

        # 生成不规则多边形顶点
        points = []
        num_vertices = random.randint(5, 8)
        for i in range(num_vertices):
            angle = (2 * np.pi * i) / num_vertices
            radius = random.randint(15, 40)
            x = int(center_x + radius * np.cos(angle))
            y = int(center_y + radius * np.sin(angle))
            x = max(0, min(IMAGE_SIZE, x))
            y = max(0, min(IMAGE_SIZE, y))
            points.append((x, y))

        draw.polygon(points, fill=fracture_color)

    # 绘制裂缝边缘
    crack_color = (random.randint(20, 40), random.randint(20, 40), random.randint(20, 40))
    for _ in range(2):
        x1 = random.randint(0, IMAGE_SIZE)
        y1 = random.randint(0, IMAGE_SIZE)
        points = [(x1, y1)]
        for _ in range(4):
            dx = random.randint(-25, 25)
            dy = random.randint(-25, 25)
            new_x = max(0, min(IMAGE_SIZE, points[-1][0] + dx))
            new_y = max(0, min(IMAGE_SIZE, points[-1][1] + dy))
            points.append((new_x, new_y))
        draw.line(points, fill=crack_color, width=2)

    # 添加模糊和噪声
    img = img.filter(ImageFilter.GaussianBlur(radius=0.8))
    pixels = np.array(img)
    noise = np.random.normal(0, 8, pixels.shape).astype(np.int16)
    pixels = np.clip(pixels.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    img = Image.fromarray(pixels)

    return img


# 生成器映射
GENERATORS = {
    'none': generate_none_image,
    'crack': generate_crack_image,
    'corrosion': generate_corrosion_image,
    'fracture': generate_fracture_image
}


def generate_dataset(output_dir, samples_per_class=250):
    """
    生成完整训练数据集

    Args:
        output_dir: 输出目录
        samples_per_class: 每类样本数量

    Returns:
        dict: 各类别生成的样本数量
    """
    stats = {}

    for class_name, generator in GENERATORS.items():
        class_dir = os.path.join(output_dir, class_name)
        os.makedirs(class_dir, exist_ok=True)

        count = 0
        for i in range(samples_per_class):
            img = generator()
            img_path = os.path.join(class_dir, f'{class_name}_{i:04d}.png')
            img.save(img_path)
            count += 1

        stats[class_name] = count
        print(f'生成 {class_name} 类图像: {count} 张')

    return stats


if __name__ == '__main__':
    # 直接运行时生成数据集
    output_path = os.path.join(os.path.dirname(__file__), '..', 'data', 'train')
    stats = generate_dataset(output_path, samples_per_class=250)
    print(f'\n数据集生成完成，共 {sum(stats.values())} 张图像')
    print(f'保存路径: {os.path.abspath(output_path)}')
