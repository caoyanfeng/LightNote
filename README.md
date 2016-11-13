# LightNote
LightNote is an open source Rich Text Editor on the Android,implemented by Span.
##说明
LightNote是一个Android系统上实现的富文本编辑器，借鉴自：https://github.com/mthli/Knife
![example.gif](./example.gif "example.gif")

区别于Knife,LightNote主要更新了以下内容：

 - 修复了添加项目符号、段落引用符号时的bug,例如，knife在下面的选中情况下，首行和末行不会添加项目符号、段落引用符号；
![image](./screenshot2.png)
 
 - 修复了超链接无法点击的bug；
 
 - 利用泛型精简优化了代码结构,复用了很多代码逻辑；
 
 - 重写了containStyle(Class, int, int, int)，bullet()、quote()等函数、降低了算法复杂度；
 
##V0.1版本
- 修复了项目符号和引用符合并存的bug,修改后，引用符号必须在项目符号之前；
- 修复了项目符号和引用符合有空行的问题bug,修改后，空行也有项目符号和引用符；
![image](./screenshot0.png)
- 加入了图片的插入功能，该功能在菜单中打开；
![image](./device-2016-11-05-213930.png)
- 加入了图片点击事件。

##apk下载
根目录下可以直接下载：https://github.com/caoyanfeng/LightNote/blob/master/app-debug.apk
