package msg.proto.protobuf.filedescriptors

import com.google.protobuf.DescriptorProtos

interface FileDescriptorSetProvider {
  fun getFileDescriptorSet(): DescriptorProtos.FileDescriptorSet
}
