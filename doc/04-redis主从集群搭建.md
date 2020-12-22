# 03-redis主从集群搭建

环境准备 redis-server：
```
192.168.25.128:6379 主  redis6379.conf
192.168.25.128:6380 从  redis6380.conf
192.168.25.128:6381 从  redis6381.conf
```

### 一、redis 一主二从
主节点 redis6379.conf 配置
```
port  6379         
daemonize  yes
bind  192.168.25.128
requirepass 123456
pidfile   /usr/local/redis/etc/redis_pid_6379.pid
logfile   /usr/local/redis/etc/redis_6379.log
dbfilename dump6379.rdb
```

从节点 redis6380.conf redis6381.conf 配置
```
port  6380 #6381         
daemonize  yes
bind  192.168.25.128  
requirepass 123456
masterauth 123456
pidfile   /usr/local/redis/etc/redis_pid_6380.pid
logfile   /usr/local/redis/etc/redis_6380.log
dbfilename dump6380.rdb
slaveof  192.168.25.128 6379
```

分别启动三个 redis 实例
```
/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis6379.conf
/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis6380.conf
/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis6381.conf
```
执行info replication查看当前主从配置
```
192.168.25.128:6379> info replication
# Replication
role:master
connected_slaves:2
slave0:ip=192.168.25.128,port=6380,state=online,offset=211,lag=1
slave1:ip=192.168.25.128,port=6381,state=online,offset=211,lag=1
master_repl_offset:211
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:2
repl_backlog_histlen:210

192.168.25.128:6380> info replication
# Replication
role:slave
master_host:192.168.25.128
master_port:6379
master_link_status:up
master_last_io_seconds_ago:9
master_sync_in_progress:0
slave_repl_offset:29
slave_priority:100
slave_read_only:1
connected_slaves:0
master_repl_offset:0
repl_backlog_active:0
repl_backlog_size:1048576
repl_backlog_first_byte_offset:0
repl_backlog_histlen:0

192.168.25.128:6381> info replication
# Replication
role:slave
master_host:192.168.25.128
master_port:6379
master_link_status:up
master_last_io_seconds_ago:8
master_sync_in_progress:0
slave_repl_offset:43
slave_priority:100
slave_read_only:1
connected_slaves:0
master_repl_offset:0
repl_backlog_active:0
repl_backlog_size:1048576
repl_backlog_first_byte_offset:0
repl_backlog_histlen:0
```
如果如上图所示没有发现从节点，很有可能是防火墙没有开放8000端口导致主从节点之间没法通信    
执行以下命令：
```
firewall-cmd --zone=public --add-port=8000/tcp --permanent
firewall-cmd --zone=public --add-port=6800/tcp --permanent
firewall-cmd --reload
```

### 二、redis 一主二从三哨兵
Redis Sentinel是Redis官方提供的集群管理工具，可以部署在其他与redis集群可通讯的机器中监控redis集群。

特性：
- 监控：能持续监控Redis的主从实例是否正常工作;
- 通知：当被监控的Redis实例出问题时，能通过API通知系统管理员或其他程序;
- 自动故障恢复：如果主实例无法正常工作，Sentinel将启动故障恢复机制把一个从实例提升为主实例，其他的从实例将会被重新配置到新的主实例，且应用程序会得到一个更换新地址的通知。

哨兵的作用：
- 监控redis(master和slave)是否正常运行;
- 当master运行出现状况，能够通知另外一个进程自动将slave切换成master。

应用场景：   
当使用redis做master-slave的高可用方案时，如果master宕机了，想自动进行主备切换，可以考虑使用哨兵模式。

配置文件 sentinel_26379.conf：
```
port 26379
daemonize yes
dir "/opt/redis-3.0.0/sentinel/tmp/26379"
logfile "/opt/redis-3.0.0/sentinel/log/sentinel_26379.log"
sentinel monitor mymaster 192.168.25.128 6379 2
sentinel down-after-milliseconds mymaster 60000
sentinel auth-pass mymaster 123456
```

启动三方 sentinel ：
```
/usr/local/redis/bin/redis-sentinel /opt/redis-3.0.0/sentinel/sentinel_26379.conf &
/usr/local/redis/bin/redis-sentinel /opt/redis-3.0.0/sentinel/sentinel_26380.conf &
/usr/local/redis/bin/redis-sentinel /opt/redis-3.0.0/sentinel/sentinel_26381.conf &
```

连接 sentinel 查看信息：
```
redis-cli -h 192.168.25.128 -p 26379 info sentinel
# Sentinel
sentinel_masters:1
sentinel_tilt:0
sentinel_running_scripts:0
sentinel_scripts_queue_length:0
master0:name=mymaster,status=ok,address=192.168.25.128:6379,slaves=2,sentinels=3
```

主节点状态：
```
192.168.25.128:6379> info replication
# Replication
role:master
connected_slaves:2
slave0:ip=192.168.25.128,port=6380,state=online,offset=170809,lag=0
slave1:ip=192.168.25.128,port=6381,state=online,offset=170809,lag=0
master_repl_offset:170952
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:2
repl_backlog_histlen:170951
```
 
手动关闭Master之后，sentinel 在监听master 确实是断线了之后，将会开始计算权值，然后重新分配主服务器    
```
3551:X 21 Dec 18:05:38.047 # +sdown master mymaster 192.168.25.128 6379
3551:X 21 Dec 18:05:38.146 # +new-epoch 1
3551:X 21 Dec 18:05:38.149 # +vote-for-leader 57ff9a9503775f11895a3d7e59ec116634c19718 1
3551:X 21 Dec 18:05:39.152 # +odown master mymaster 192.168.25.128 6379 #quorum 3/2
3551:X 21 Dec 18:05:39.152 # Next failover delay: I will not start a failover before Mon Dec 21 18:11:38 2020
3551:X 21 Dec 18:05:39.267 # +config-update-from sentinel 192.168.25.128:26380 192.168.25.128 26380 @ mymaster 192.168.25.128 6379
3551:X 21 Dec 18:05:39.267 # +switch-master mymaster 192.168.25.128 6379 192.168.25.128 6381
3551:X 21 Dec 18:05:39.268 * +slave slave 192.168.25.128:6380 192.168.25.128 6380 @ mymaster 192.168.25.128 6381
3551:X 21 Dec 18:05:39.274 * +slave slave 192.168.25.128:6379 192.168.25.128 6379 @ mymaster 192.168.25.128 6381
3551:X 21 Dec 18:06:39.315 # +sdown slave 192.168.25.128:6379 192.168.25.128 6379 @ mymaster 192.168.25.128 6381
```

6381 被选举为 Master
```
redis-cli -h 192.168.25.128 -p 26380 info sentinel
# Sentinel
sentinel_masters:1
sentinel_tilt:0
sentinel_running_scripts:0
sentinel_scripts_queue_length:0
master0:name=mymaster,status=ok,address=192.168.25.128:6381,slaves=2,sentinels=3
```

重新启动之前的主节点 6379
如果 6379 重连之后，会不会抢回属于他的位置，答案是否定的，因此当 6379 回来之后，也只能当个从服务
```
redis-cli -h 192.168.25.128 -p 6379 -a 123456
192.168.25.128:6379> info replication
# Replication
role:slave
master_host:192.168.25.128
master_port:6381
master_link_status:down
master_last_io_seconds_ago:-1
master_sync_in_progress:0
slave_repl_offset:1
master_link_down_since_seconds:1608603251
slave_priority:100
slave_read_only:1
connected_slaves:0
master_repl_offset:0
repl_backlog_active:0
repl_backlog_size:1048576
repl_backlog_first_byte_offset:0
repl_backlog_histlen:0
```

Sentinel的工作方式:
- 每个Sentinel以每秒钟一次的频率向它所知的Master，Slave以及其他 Sentinel 实例发送一个 PING 命令 
- 如果一个实例（instance）距离最后一次有效回复 PING 命令的时间超过 down-after-milliseconds 选项所指定的值， 则这个实例会被 Sentinel 标记为主观下线。 
- 如果一个Master被标记为主观下线，则正在监视这个Master的所有 Sentinel 要以每秒一次的频率确认Master的确进入了主观下线状态。 
- 当有足够数量的 Sentinel（大于等于配置文件指定的值）在指定的时间范围内确认Master的确进入了主观下线状态， 则Master会被标记为客观下线 
- 在一般情况下， 每个 Sentinel 会以每 10 秒一次的频率向它已知的所有Master，Slave发送 INFO 命令 
- 当Master被 Sentinel 标记为客观下线时，Sentinel 向下线的 Master 的所有 Slave 发送 INFO 命令的频率会从 10 秒一次改为每秒一次 
- 若没有足够数量的 Sentinel 同意 Master 已经下线， Master 的客观下线状态就会被移除。若 Master 重新向 Sentinel 的 PING 命令返回有效回复， Master 的主观下线状态就会被移除。


### 三、redis 集群搭建
Redis 集群采用了P2P的模式，完全去中心化。Redis 把所有的 Key 分成了 16384 个 slot，每个 Redis 实例负责其中一部分 slot 。集群中的所有信息（节点、端口、slot等），都通过节点之间定期的数据交换而更新。

Redis 客户端可以在任意一个 Redis 实例发出请求，如果所需数据不在该实例中，通过重定向命令引导客户端访问所需的实例。

安装集群所需软件
```
yum install ruby
yum install rubygems
gem install redis 
```

创建集群命令：
```
/usr/local/redis/bin/redis-trib.rb create --replicas 1 192.168.25.128:6379 192.168.25.128:6380 192.168.25.128:6381 192.168.25.128:6382 192.168.25.128:6383 192.168.25.128:6384 
```
调用 ruby 命令来进行创建集群，--replicas 1 表示主从复制比例为 1:1，即一个主节点对应一个从节点；然后，默认给我们分配好了每个主节点和对应从节点服务，以及 solt 的大小，因为在 Redis 集群中有且仅有 16383 个 solt ，默认情况会给我们平均分配，当然你可以指定，后续的增减节点也可以重新分配。

验证：   
```
redis-cli -c -h 192.168.25.128 -p 9001
cluster info
cluster nodes
```
通过命令，可以详细的看出集群信息和各个节点状态，主从信息以及连接数、槽信息等。     
当我们 set key1 value1 时，出现了 Redirected to slot 信息并自动连接到了9002节点。这也是集群的一个数据分配特性，这里不详细说了。

