调试开发及部署环境要求
    1.eclipse开发及调试环境要求
        1.1 安装wtp环境
            1.1.1 eclipse中安装wtp环境的update地址:http://download.eclipse.org/webtools/repository/luna
            1.1.2 wtp环境配置及安装说明
                        文档1:http://www.vogella.com/tutorials/EclipseWTP/article.html#overview(说明相对完整)
                        文档2:http://dev.antoinesolutions.com/web-tools-platform-wtp
        1.3 安装maven
        1.4 安装Tomcat 6.0以上
        1.5 安装Redis(开发环境依赖本地redis)

    2.部署环境步骤
        2.1 部署前通过maven命令执行编译
            2.1.1 maven编译的具体操作:切换到pom.xml所在的工程根目录,执行命令:mvn package. 如果命令行提示窗口中出现"BUILD SUCCESS",则表明编译成功.
        2.2 maven编译成功后,将target目录下的网站文件拷贝到tomcat主目录下(默认为root目录).如果不通过这种方式也可以通过配置文件,手工
            设置tomcat的主目录,具体操作可参考:http://blog.knowsky.com/188854.htm  或者 http://blog.csdn.net/songhuanren/article/details/3301615
        2.3 手工拷贝maven依赖的本地文件(工程目录libs下面的所有jar包)到网站目录中的WEB-INF中的lib下面
        2.4 启动Tomcat,输入http://localhost:8080/index.html访问,能够访问到界面,说明部署配置成功.

特别说明:
    1.由于爬虫项目打包后,在读取配置文件中的redis地址的问题,一直找不到解决方法,所以项目中关于redis的配置信息是写在代码里的,
      实际部署的时候需要手工更改代码中的redis地址.