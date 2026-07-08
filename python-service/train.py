"""
CNN模型训练脚本

功能:
1. 生成合成训练数据
2. 训练DefectCNN模型
3. 保存模型权重和训练历史

使用方法:
    python train.py [--epochs 50] [--batch_size 32] [--samples 250]
"""

import sys
import os
import argparse
import json
import time
from datetime import datetime

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, Dataset
from torchvision import transforms
from PIL import Image

# 添加项目路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from models import DefectCNN, CLASS_NAMES, NUM_CLASSES
from utils.data_generator import generate_dataset


class DefectDataset(Dataset):
    """缺陷图像数据集"""

    def __init__(self, data_dir, transform=None):
        self.data_dir = data_dir
        self.transform = transform
        self.images = []
        self.labels = []

        # 加载所有图像路径和标签
        for label_idx, class_name in enumerate(CLASS_NAMES):
            class_dir = os.path.join(data_dir, class_name)
            if not os.path.exists(class_dir):
                continue

            for img_name in os.listdir(class_dir):
                if img_name.endswith(('.png', '.jpg', '.jpeg')):
                    self.images.append(os.path.join(class_dir, img_name))
                    self.labels.append(label_idx)

    def __len__(self):
        return len(self.images)

    def __getitem__(self, idx):
        img_path = self.images[idx]
        label = self.labels[idx]

        # 加载图像
        image = Image.open(img_path).convert('RGB')

        if self.transform:
            image = self.transform(image)

        return image, label


def get_transforms(is_training=True):
    """获取数据预处理管道"""
    if is_training:
        return transforms.Compose([
            transforms.Resize((128, 128)),
            transforms.RandomHorizontalFlip(),
            transforms.RandomVerticalFlip(),
            transforms.RandomRotation(10),
            transforms.ColorJitter(brightness=0.2, contrast=0.2),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                               std=[0.229, 0.224, 0.225])
        ])
    else:
        return transforms.Compose([
            transforms.Resize((128, 128)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                               std=[0.229, 0.224, 0.225])
        ])


def train_epoch(model, dataloader, criterion, optimizer, device):
    """训练一个epoch"""
    model.train()
    running_loss = 0.0
    correct = 0
    total = 0

    for images, labels in dataloader:
        images, labels = images.to(device), labels.to(device)

        # 前向传播
        outputs = model(images)
        loss = criterion(outputs, labels)

        # 反向传播
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        # 统计
        running_loss += loss.item() * images.size(0)
        _, predicted = torch.max(outputs.data, 1)
        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    epoch_loss = running_loss / total
    epoch_acc = correct / total

    return epoch_loss, epoch_acc


def validate(model, dataloader, criterion, device):
    """验证模型"""
    model.eval()
    running_loss = 0.0
    correct = 0
    total = 0

    with torch.no_grad():
        for images, labels in dataloader:
            images, labels = images.to(device), labels.to(device)

            outputs = model(images)
            loss = criterion(outputs, labels)

            running_loss += loss.item() * images.size(0)
            _, predicted = torch.max(outputs.data, 1)
            total += labels.size(0)
            correct += (predicted == labels).sum().item()

    val_loss = running_loss / total
    val_acc = correct / total

    return val_loss, val_acc


def main():
    parser = argparse.ArgumentParser(description='训练管道缺陷检测CNN模型')
    parser.add_argument('--epochs', type=int, default=30, help='训练轮数')
    parser.add_argument('--batch_size', type=int, default=32, help='批次大小')
    parser.add_argument('--lr', type=float, default=0.001, help='学习率')
    parser.add_argument('--samples', type=int, default=250, help='每类样本数量')
    parser.add_argument('--no-generate', action='store_true', help='跳过数据生成')
    args = parser.parse_args()

    # 设备配置
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f'使用设备: {device}')

    # 路径配置
    base_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(base_dir, 'data', 'train')
    model_dir = os.path.join(base_dir, 'checkpoints')
    os.makedirs(model_dir, exist_ok=True)

    # 步骤1: 生成训练数据
    if not args.no_generate:
        print('\n=== 步骤1: 生成合成训练数据 ===')
        stats = generate_dataset(data_dir, samples_per_class=args.samples)
        print(f'数据生成完成: {stats}')

    # 步骤2: 加载数据集
    print('\n=== 步骤2: 加载数据集 ===')
    train_transform = get_transforms(is_training=True)
    val_transform = get_transforms(is_training=False)

    # 分割训练集和验证集 (80/20)
    full_dataset = DefectDataset(data_dir, transform=train_transform)
    total_size = len(full_dataset)
    train_size = int(0.8 * total_size)
    val_size = total_size - train_size

    train_dataset, val_dataset = torch.utils.data.random_split(
        full_dataset, [train_size, val_size]
    )

    # 为验证集使用不同的transform
    val_dataset.dataset = DefectDataset(data_dir, transform=val_transform)

    train_loader = DataLoader(train_dataset, batch_size=args.batch_size,
                             shuffle=True, num_workers=0)
    val_loader = DataLoader(val_dataset, batch_size=args.batch_size,
                           shuffle=False, num_workers=0)

    print(f'训练集大小: {train_size}, 验证集大小: {val_size}')

    # 步骤3: 初始化模型
    print('\n=== 步骤3: 初始化模型 ===')
    model = DefectCNN(num_classes=NUM_CLASSES).to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=args.lr)
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=10, gamma=0.1)

    print(f'模型参数量: {sum(p.numel() for p in model.parameters()):,}')

    # 步骤4: 训练模型
    print('\n=== 步骤4: 开始训练 ===')
    history = {
        'train_loss': [],
        'train_acc': [],
        'val_loss': [],
        'val_acc': []
    }

    best_val_acc = 0.0
    start_time = time.time()

    for epoch in range(args.epochs):
        # 训练
        train_loss, train_acc = train_epoch(
            model, train_loader, criterion, optimizer, device
        )

        # 验证
        val_loss, val_acc = validate(model, val_loader, criterion, device)

        # 更新学习率
        scheduler.step()

        # 记录历史
        history['train_loss'].append(train_loss)
        history['train_acc'].append(train_acc)
        history['val_loss'].append(val_loss)
        history['val_acc'].append(val_acc)

        # 打印进度
        print(f'Epoch [{epoch+1}/{args.epochs}] '
              f'Train Loss: {train_loss:.4f} Acc: {train_acc:.4f} | '
              f'Val Loss: {val_loss:.4f} Acc: {val_acc:.4f}')

        # 保存最佳模型
        if val_acc > best_val_acc:
            best_val_acc = val_acc
            torch.save({
                'epoch': epoch,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'val_acc': val_acc,
                'val_loss': val_loss,
            }, os.path.join(model_dir, 'best_model.pth'))
            print(f'  -> 保存最佳模型 (验证准确率: {val_acc:.4f})')

    training_time = time.time() - start_time

    # 步骤5: 保存训练结果
    print('\n=== 步骤5: 保存训练结果 ===')

    # 保存最终模型
    torch.save({
        'model_state_dict': model.state_dict(),
        'class_names': CLASS_NAMES,
        'num_classes': NUM_CLASSES,
    }, os.path.join(model_dir, 'final_model.pth'))

    # 保存训练历史
    history_path = os.path.join(model_dir, 'training_history.json')
    with open(history_path, 'w') as f:
        json.dump(history, f, indent=2)

    # 打印训练总结
    print('\n' + '='*50)
    print('训练完成!')
    print(f'训练时间: {training_time:.1f}秒')
    print(f'最佳验证准确率: {best_val_acc:.4f}')
    print(f'模型保存路径: {model_dir}')
    print('='*50)


if __name__ == '__main__':
    main()
