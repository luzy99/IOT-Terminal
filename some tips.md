## M5stack 串口工具

使用amp工具访问板子存储空间

```bash
pip install adafruit-ampy
```

以下两种是基于amp的shell工具

```bash
pip install mpfshell
```

```bash
pip install rshell
```

Linux下

```bash
rshell -p /dev/tty.SLAB_USBtoUART
```

Windows下

```bash
rshell -p com3
```

windows下我试了一遍只有rshell能读取目录，`cd /flash`访问板子根目录。

输入`repl` 可以直接进入REPL的MicroPython命令提示行 （`ctrl X`退出）。

要注意的是在Windows上映射的根目录是C盘，貌似无法cd到别的盘，因此如果要复制文件到板子，需要先把文件放到C盘下。（写入过程可能比较慢）

## mqtt服务器搭建

centos上直接用yum安装

```bash
yum-config-manager --add-repo https://repos.emqx.io/emqx-ce/redhat/centos/7/emqx-ce.repo
yum install emqx
```

启动：

```bash
systemctl start emqx
```

开机自启

```bash
systemctl enable emqx
```

此时就可以通过`ip地址:18083`访问控制台了

默认用户名为`admin`，密码为`public`，进入后可修改语言，修改密码。