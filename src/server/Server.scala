package server

import java.rmi.server.UnicastRemoteObject
import java.rmi.registry.Registry
import java.rmi.registry.LocateRegistry
import scala.language.postfixOps
import com.redis._

class Server extends ServerTrait {
  val db: RedisClient = new RedisClient("localhost",6379)

  def createUser(user: String, pass: String): Boolean = {
    val dbUser = s"TWEETORRO:USERS:$user"
    if (db.exists(s"$dbUser:PASSWORD"))
      false
    else {
      db.set(s"$dbUser:PASSWORD",pass)
      db.set(s"$dbUser:ONLINE", false)

    }
  }

  def login(user: String, pass: String) : Boolean = {
    val db_user = s"TWEETORRO:USERS:$user:PASSWORD"
    if (db.exists(db_user)) {
      val user_pass = db.get(db_user)
      user_pass match {
        case Some(value) =>
          value == pass
          val onLine= s"TWEETORRO:USERS:$user:ONLINE"
          db.set(onLine,true)
        case None =>
          false
      }
    } else
      false
  }

}

object Server {
  def main(args: Array[String]): Unit = {
    
    try {
      // val server: ServerTrait = new Server
      val server: ServerTrait = new Server
      val stub = UnicastRemoteObject.exportObject(server,0).asInstanceOf[ServerTrait]
      val registry = LocateRegistry.createRegistry(1099)
      registry.rebind("tweetorro", stub)
      
      println("Server ready :D")
    }
    catch {
      case e: Exception => e printStackTrace
    }
    
  }
}
