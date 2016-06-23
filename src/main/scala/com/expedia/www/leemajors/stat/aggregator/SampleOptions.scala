package com.expedia.www.leemajors.stat.aggregator

import com.expedia.spark.streaming.common.options._

class SampleOptions extends StreamingOptions with ZookeeperOptions with KafkaOptions with CollectorOptions {

}