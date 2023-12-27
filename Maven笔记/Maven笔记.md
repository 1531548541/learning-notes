maven将jar包做成maven依赖

~~~sh
mvn install:install-file -Dfile=./target/datax/datax/lib/datax-common-0.0.1-SNAPSHOT.jar -DgroupId=com.alibaba.datax -DartifactId=common -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar
~~~

