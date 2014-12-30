调试开发及部署环境要求
    1.eclipse开发及调试环境要求
        1.1 安装wtp环境
            1.1.1 eclipse中安装wtp环境的update地址:http://download.eclipse.org/webtools/repository/luna
            1.1.2 wtp环境配置及安装说明:http://dev.antoinesolutions.com/web-tools-platform-wtp
        1.3 安装maven
        1.4 安装Tomcat 6.0以上
        1.5 安装Redis(开发环境依赖本地redis)

    2.部署环境步骤
        2.1 部署前通过maven命令执行编译,确保工程能够编译成功
        2.2 maven编译成功后,将target目录下的网站文件拷贝到tomcat主目录下(默认为root目录).如果不通过这种方式也可以通过配置文件,手工
            设置tomcat的主目录,具体操作可参考:http://blog.knowsky.com/188854.htm  或者 http://blog.csdn.net/songhuanren/article/details/3301615
        2.3 手工拷贝maven依赖的本地文件到网站目录中的WEB-INF中的lib下面
        2.4 启动Tomcat,输入http://localhost:8080/index.html访问,能够访问到界面,说明部署配置成功.