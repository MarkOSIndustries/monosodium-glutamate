package kat.kafka

class NoSuchTopicException(topic:String) : Exception("Couldn't find partitions for topic $topic")
