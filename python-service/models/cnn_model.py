import torch
import torch.nn as nn
import torch.nn.functional as F

# 缺陷分类类别
CLASS_NAMES = ['none', 'crack', 'corrosion', 'fracture']
NUM_CLASSES = len(CLASS_NAMES)


class DefectCNN(nn.Module):
    """
    管道缺陷图像分类CNN模型

    网络结构:
    - 3个卷积块，每块包含 Conv2d + BatchNorm + ReLU + MaxPool
    - 2个全连接层 + Dropout
    - 输入: 3 x 128 x 128 RGB图像
    - 输出: 4类缺陷概率 (none/crack/corrosion/fracture)
    """

    def __init__(self, num_classes=NUM_CLASSES):
        super(DefectCNN, self).__init__()

        # 卷积块1: 3 -> 32
        self.conv1 = nn.Conv2d(3, 32, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm2d(32)
        self.pool1 = nn.MaxPool2d(2, 2)

        # 卷积块2: 32 -> 64
        self.conv2 = nn.Conv2d(32, 64, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm2d(64)
        self.pool2 = nn.MaxPool2d(2, 2)

        # 卷积块3: 64 -> 128
        self.conv3 = nn.Conv2d(64, 128, kernel_size=3, padding=1)
        self.bn3 = nn.BatchNorm2d(128)
        self.pool3 = nn.MaxPool2d(2, 2)

        # 全连接层
        # 128 x 16 x 16 = 32768
        self.fc1 = nn.Linear(128 * 16 * 16, 256)
        self.dropout = nn.Dropout(0.5)
        self.fc2 = nn.Linear(256, num_classes)

    def forward(self, x):
        # 卷积块1
        x = self.pool1(F.relu(self.bn1(self.conv1(x))))

        # 卷积块2
        x = self.pool2(F.relu(self.bn2(self.conv2(x))))

        # 卷积块3
        x = self.pool3(F.relu(self.bn3(self.conv3(x))))

        # 展平
        x = x.view(x.size(0), -1)

        # 全连接
        x = F.relu(self.fc1(x))
        x = self.dropout(x)
        x = self.fc2(x)

        return x


def get_model(num_classes=NUM_CLASSES):
    """获取模型实例"""
    return DefectCNN(num_classes=num_classes)
