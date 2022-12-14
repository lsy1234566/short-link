
> ⭐基于git项目二次开发: https://github.com/zjcscut/octopus⭐

##!!!请观看👆项目后再看本文!!!

当作为实际使用还是有点不够:
整体设计:
1. redis做了缓存,没有用到布隆过滤器
2. rabbit异步入库访问记录
3. Scheduling定时任务做了统计

缺点:
1. 生成和统计的数据缺少业务标识(根据业务划分短链)
2. 生成短链方案可以优化,它是靠数据库唯一性约束碰撞(碰撞概率不会太大)


### 短链问题
1. 微博和Twitter都有140字数的限制，如果分享一个长网址，很容易就超出限制。
2. 营销短信，字数的限制,当字数过长: 1.不美观 2.超出字符额外收费。
3. 生成二维码的原始链接,当原始链接过长时,生成的二维码过于复杂,导致一些像素较低的手机无法扫描.


### 目标结果
1. 高性能
2. 合理长度
3. 访问统计

###  核心设计

**对于进入系统的请求经过处理后改写Response流重定向到指定网(系统只做转发和一些过滤功能)**

##### 1. 转换
最普遍也是最好用的方案就是高进制转换,通常以a-z A-Z 0-9作为进制转换(62个字符)

> 111->(转换进制)oNWxUYwrXdCOIj4ck6M8RbiQa3H91pSmZTAh70zquLnKvt2VyEGlBsPJgDe5Ff=>cb

通常来说短链我们不需要具备可读性,以越短越好而存在,如移动的4位、美团12,在不考虑短链接回收,6位应该是比较合理的存在,而美团的12为应该是直接用雪花进行高进制转换直接使用。
> N = 4，组合数为62 ^ 4 = 14_776_336，147万接近148万
N = 5，组合数为62 ^ 5 = 916_132_832，9.16亿左右
N = 6，组合数为62 ^ 6 = 56_800_235_584，568亿左右
N = 12，组合数为62 ^ 12 = 3,226,266,762,397,899,821,056，3,226,266,762,397亿左右
>但是需要注意的是这种计算方法是:1位-4位有147万、1位-6位有58亿
>如果希望始终保持在6位那应该是568亿-9.16亿 = 558亿-绰绰有余

转换方案呢? 不得提供一个数值比如用主键、雪花算法去参与到转换中?
目前多数是使用的雪花算法然后给予一定的压缩或者裁剪,笔者基于的项目也是这种方案
但是了解雪花算法的同学应该知道**雪花算法有两个缺陷**
1. 需要设置workerId和dataCenterId(当然美团Leaf可以解决)
2. 雪花生成的是定长,后续需要二次压缩,无法避免碰撞

> 雪花算法固定生成19位数字(6980725272719003680)经过62进制压缩后11位(X8LEbYzwI8w)

使用常见**主键有一个严重**
1. 过于频繁的对数据库操作

> redis自增就不讲啦,丢失后就太难处理了

**笔者推荐方案:**
基于数据库申请一批次的**主键**(应该叫做编号), 如一个业务标识位的10的,申请了一批10000-11000的主键,那本批主键就已经被占用了,该方法避免了使用插入自增的方式带来的数据库压力,数据库的频率从1减小到了1/step
![在这里插入图片描述](https://img-blog.csdnimg.cn/71c9593695464f25beebad1f8d341f33.png)

> 对于占用的主键范围只在内存中,即使丢失了下次申请即可

![在这里插入图片描述](https://img-blog.csdnimg.cn/484b35b98355462f93584abcc86697d1.png)
但是对于这种方式取到的主键有一个缺点,如1-1000生成出来的一千个短链接码如果没有经过处理会变成非固定长度,大概率只会是1位或者两位的长度,当然在加上一些填充码或者设置起始编号就可以合理解决(从)

> 1=>a
> 2=>b
> 3=>c
56,800,235,584

##### 2. 存储

> 我们需要记录请求记录以用于后续记录pv uv等数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/34ef03037c5541dc9440d37e710e9e8b.png)

######  2.1 基于MQ

> 基于MQ的方案是最优秀的异步、安全。但对于一个基础系统引入MQ会使其庞大、重。也有许多公司MQ并不是主要方案

######  2.2 同步记录DB

> 基于DB直接记录安全但性能打折大半，本来只要做一些基于redis的过滤操作，引入DB后整个系统会变的缓慢。

######  2.2 异步记录DB-默认使用

> 在Spring2.3后，框架的关闭变的优雅，使其在本机异步批量记录有一定可能，同时也是因为访问记录丢失几条问题不大的前提下
> 兼顾轻便、性能

具体查看:https://blog.csdn.net/qq_35040959/article/details/127464968

```java
    //访问记录
    private final LinkedBlockingQueue<TransformEvent> linkedBlockingQueue = new LinkedBlockingQueue(Integer.MAX_VALUE);

    @Override
    public void start() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        //容器关闭监听-处理未记录的访问记录
        springShutdownEventListening.setListening(execTask);
        //每0.2秒处理一次批任务
        scheduledExecutorService.scheduleWithFixedDelay(execTask, 200, 200, TimeUnit.MILLISECONDS);
    }
```
##### 3. 功能
1. 响应式框架
2. 黑名单拦截
3. 访问域名拦截
4. 白名单api配置
5. redis缓存
6. pvuv等报表记录
7. mq异步记录访问-可选
8. 本地异步记录访问-可选-默认
9. 不重复高性能压缩码-可选-默认
10. 本地缓存-升级中
11. 布隆过滤器-可升级