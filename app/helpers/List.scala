package helpers

class List[T] {
  private var seq: Seq[T] = Seq[T]()

  def clear(): Unit = {
    seq = Seq[T]()
  }

  def append(item: T): Unit = {
    seq = seq :+ item
  }

  def isEmpty: Boolean = {
    seq.isEmpty
  }

  def getAll: Seq[T] = seq
}