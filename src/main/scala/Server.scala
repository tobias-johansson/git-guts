import Git._
import com.twitter.finagle.Http
import com.twitter.finagle.http.Response
import com.twitter.io.Reader
import com.twitter.util.{Await, Future}
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import org.eclipse.jgit.lib.Repository

class Server(repo: Repository) {

  object files {
    def file(path: String, contentType: String = "text/html"): Future[Response] = {
      //      Reader.readAll(Reader.fromFile(new File(path))).map { content =>
      val stream = getClass.getResourceAsStream(path)
      Reader.readAll(Reader.fromStream(stream)).map { content =>
        val rep = Response()
        rep.content = content
        rep.contentType = contentType
        rep
      }
    }

    val page: Endpoint[Response] = get("static" :: string) { name: String =>
      file(name)
    }
  }

  object nodes {
    case class Nodes(nodes: Seq[Node])
    case class Node(id: String, label: String, kind: String, level: Int)

    val ep: Endpoint[Nodes] = get("api" :: "nodes") {
//      val commits = Git.commits(repo).toSeq.reverse
//      val ins = commits.foldLeft(Map[String, Int]().withDefaultValue(0)) { (map, c) =>
//        c.parents.toSeq.foldLeft(map) { (map, p) =>
//          map.updated(p, map(p) + 1)
//        }
//      }
//      val levels = commits.foldLeft(Map[String, Int]().withDefaultValue(0)) { (map, c) =>
//          val max = c.parents.map(map).max
//          map.updated(c.id, )
//        }
//      }
      val hs = Git.heads(repo)
      val cs = hs.zipWithIndex.flatMap {
        case (r, i) =>
          val rn = Node(r.getName, r.getName, "ref", i)
          Git.commits(repo, r).toSeq.map(c => Node(c.id, c.message, "commit", i))
//          rn +: cs
      }.foldLeft(Map[String, Node]()) { (seen, n) =>
          if (seen.isDefinedAt(n.id)) seen else seen.updated(n.id, n)
        }
        .values
        .toSeq
      val level = cs.map(_.level).max
      val ts    = Git.trees(repo).toSeq.map(t => Node(t.id, "", "tree", level + 1))
      val fs    = Git.files(repo).toSeq.distinct.map(f => Node(f.id, f.content, "file", level + 2))
      Ok(Nodes(cs ++ ts ++ fs))
    }
  }

  object edges {
    case class Edges(edges: Seq[Edge])
    case class Edge(from: String, to: String, label: String, kind: String)

    val ep: Endpoint[Edges] = get("api" :: "edges") {
      val cs = Git.commitEdges(repo).toSeq.map(e => Edge(e.src, e.dst, "", "commit"))
      val ts = Git.treeEdges(repo).toSeq.map(e => Edge(e.src, e.dst, "", "tree"))
      val fs = Git.fileEdges(repo).toSeq.map(e => Edge(e.src, e.dst, e.path, "file"))
      Ok(Edges(cs ++ ts ++ fs))
    }
  }

  def serve() =
    Await.ready(
      Http.server.serve(":9999", (files.page :+: nodes.ep :+: edges.ep).toServiceAs[Application.Json])
    )
}
