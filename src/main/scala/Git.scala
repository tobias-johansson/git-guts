import java.io.File
import java.nio.charset.StandardCharsets

import org.eclipse.jgit.lib.{ObjectId, Ref, Repository}
import org.eclipse.jgit.revwalk.{RevCommit, RevTree, RevWalk}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk

import scala.collection.JavaConverters._
object Git {

  object Data {
    type CommitId = String
    type TreeId   = String
    type FileId   = String

    case class Commit(id: CommitId, parents: Set[String], message: String)
    case class Tree(id: TreeId)
    case class File(id: FileId, content: String)

    case class CommitEdge(src: CommitId, dst: CommitId)
    case class TreeEdge(src: CommitId, dst: TreeId)
    case class FileEdge(src: TreeId, dst: FileId, path: String)
  }

  def revWalk(repo: Repository): Iterable[RevCommit] = {
    revWalk(repo, repo.getAllRefs.asScala.values)
  }

  def revWalk(repo: Repository, heads: Iterable[Ref]): Iterable[RevCommit] = {
    val walk = new RevWalk(repo)
    heads foreach { ref =>
      walk.markStart(walk.parseCommit(ref.getObjectId()))
    }
    walk.asScala
  }

  def treeWalk[A](repo: Repository, tree: RevTree, get: TreeWalk => A) = {
    val treeWalk = new TreeWalk(repo)
    treeWalk.addTree(tree)
    treeWalk.setRecursive(true)
    new Iterator[A] {
      var more                      = treeWalk.next()
      override def hasNext: Boolean = more
      override def next(): A = {
        val elem = get(treeWalk)
        more = treeWalk.next()
        elem
      }
    }
  }

  def heads(repo: Repository) = repo.getAllRefs.asScala.values

  def commits(repo: Repository, ref: Ref) =
    for {
      commit <- revWalk(repo, Seq(ref))
    } yield
      Data.Commit(id = commit.getName,
                  parents = commit.getParents.map(_.getName).toSet,
                  message = commit.getShortMessage)

  def commitEdges(repo: Repository) =
    for {
      commit <- revWalk(repo)
      parent <- commit.getParents
    } yield Data.CommitEdge(src = commit.getName, dst = parent.getName)

  def trees(repo: Repository) =
    for {
      commit <- revWalk(repo)
    } yield Data.Tree(id = commit.getTree.getName)

  def treeEdges(repo: Repository) =
    for {
      commit <- revWalk(repo)
    } yield Data.TreeEdge(src = commit.getName, dst = commit.getTree.getName)

  def files(repo: Repository) =
    for {
      commit <- revWalk(repo)
      fileId <- treeWalk(repo, commit.getTree, _.getObjectId(0))
    } yield Data.File(id = fileId.name, content = read(repo, fileId))

  def read(repo: Repository, id: ObjectId): String = {
    new String(
      repo.open(id).getBytes(),
      StandardCharsets.UTF_8
    )
  }

  def fileEdges(repo: Repository) =
    for {
      commit         <- revWalk(repo)
      (path, fileId) <- treeWalk(repo, commit.getTree, w => (w.getPathString, w.getObjectId(0).name))
    } yield Data.FileEdge(src = commit.getTree.getName, dst = fileId, path = path)

}
