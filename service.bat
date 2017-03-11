set classpath=%classpath%;.\target\dependency\*;.\target\busRouteFinder-1.0.jar

java -XX:-UseParallelOldGC -Xms512m -Xmx1g org.gokhanka.busprovider.busRouteFinder.App %1 %2
