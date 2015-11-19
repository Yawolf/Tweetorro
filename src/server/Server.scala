package server

import java.rmi.server.UnicastRemoteObject
import java.rmi.registry.Registry
import java.rmi.registry.LocateRegistry
import scala.language.postfixOps
import com.redis._
import sun.util.calendar.JulianCalendar.Date
import java.util.Calendar

class Server extends ServerTrait {
  val db: RedisClient = new RedisClient("localhost",6379)
  
  def searchUsers(name: String) : List[String] = {
    db.lrange("TWEETORRO:USERS", 0, -1).getOrElse(List()).flatten.filter {x => x.contains(name)}
  }
  
  def sendDM(DM:(String,String,String),userTo: String) : Boolean = {
    val user = DM._1
    val id = db.incr("TWEETORRO:DMID") getOrElse " "
    db.lpush(s"TWEETORRO:USERS:$userTo:DM", id)
    db.lpush(s"TWEETORRO:USERS:$user:DM", id)
    db.set(s"TWEETORRO:DM:$id:ID",id)
    db.set(s"TWEETORRO:DM:$id:USER",user)
    db.set(s"TWEETORRO:DM:$id:MESSAGE",DM._2)
    db.set(s"TWEETORRO:DM:$id:DATE",DM._3)
    true
  }
  
  def getDM(user: String, number: Int) : List[(String, String, String, String)] = {
    val listaIDs = db.lrange(s"TWEETORRO:USERS:$user:DM", 0, -1)
    listaIDs.getOrElse(List()).flatten.map(getDMTuple(_)).sortBy(_._4).take(number)
  }
  
  def getDMTuple(DM: String): (String,String,String,String) = {
    (db.get(s"TWEETORRO:DM:$DM:ID").get,
    db.get(s"TWEETORRO:DM:$DM:USER").get,
    db.get(s"TWEETORRO:DM:$DM:MESSAGE").get,
    db.get(s"TWEETORRO:DM:$DM:DATE").get)
  }
  
  def getTweets(user: String,number: Int): List[(String, String, String, String)] = {
    val listaIDs = db.lrange(s"TWEETORRO:USERS:$user:TWEETS",0,-1)
    listaIDs.getOrElse(List()).flatten.map(getTweetTuple(_)).sortBy(_._4).take(number)
  }

  def getTweetTuple(tweet: String): (String, String, String, String) = {
    (db.get(s"TWEETORRO:TWEETS:$tweet:ID").get,
    db.get(s"TWEETORRO:TWEETS:$tweet:USER").get,
    db.get(s"TWEETORRO:TWEETS:$tweet:MESSAGE").get,
    db.get(s"TWEETORRO:TWEETS:$tweet:DATE").get)
  }
  
  def sendTweet(tweet: (String, String, String)): Boolean = {
    val id = db.incr("TWEETORRO:tweetID") getOrElse " "
    db.lpush(s"TWEETORRO:USERS:${tweet._1}:TWEETS", s"tweet$id")
    val lista = db.lrange(s"TWEETORRO:USERS:$tweet._1:FOLLOWERS", 0,-1)
    lista.getOrElse(List()).flatten.foreach{x => db.lpush(s"TWEETORRO:USERS:$x:TWEETS", s"tweet$id")}
    db.set(s"TWEETORRO:TWEETS:tweet$id:USER", tweet._1)
    db.set(s"TWEETORRO:TWEETS:tweet$id:MESSAGE",tweet._2)
    db.set(s"TWEETORRO:TWEETS:tweet$id:DATE",tweet._3)
    db.set(s"TWEETORRO:TWEETS:tweet$id:ID",id)
    true
  }
  
  def retweet(user: String, tweetID: String): Boolean = {
    if (db.get(s"TWEETORRO:TWEETS:$tweetID:user") != user){
      val lista = db.lrange(s"TWEETORRO:USERS:$user:FOLLOWERS", 0,-1)
      lista.getOrElse(List()).flatten.foreach { x =>
        if (!db.lrange("TWEETORRO:USERS:$x:TWEETS", 0, -1).get.contains(tweetID))
          db.lpush(s"TWEETORRO:USERS:$x:TWEETS", tweetID)
      }
      db.lpush(s"TWEETORRO:USERS:$user:TWEETS", tweetID)
      true
    }else{
      false
    }
  }
  
  def followers(user: String,number: Int): List[String] = {
    val lista = db.lrange(s"TWEETORRO:USERS:$user:FOLLOWERS", 0,number)
    lista.getOrElse(List()).flatten
  }
  
  def following(user: String,number: Int): List[String] = {
    val lista = db.lrange(s"TWEETORRO:USERS:$user:FOLLOWING", 0,number)
    lista.getOrElse(List()).flatten
  }
  
  def logoutRemote(user: String): Boolean = {
    db.set(s"TWEETORRO:USERS:$user:LOGGED",false)
    true
  }
  def createUser(user: String, pass: String): Boolean = {
    val dbUser = s"TWEETORRO:USERS:$user"
    if (db.exists(s"$dbUser:PASSWORD"))
      false
    else {
      db.lpush(s"TWEETORRO:USERS", user) //Insertamos el nombre de la persona en la lista de usuarios
      db.set(s"$dbUser:PASSWORD",pass)
      db.set(s"$dbUser:LOGGED", false)
    }
  }

  def login(user: String, pass: String) : Boolean = {
    val dbUserPass = s"TWEETORRO:USERS:$user:PASSWORD"
    if (db.exists(dbUserPass)) {
      val user_pass = db.get(dbUserPass)
      user_pass match {
        case Some(value) =>
          (value == pass) && db.set(s"TWEETORRO:USERS:$user:LOGGED",true)
        case None =>
          false
      }
    } else
      false
  }
  
  def checkRemoteProfile(user: String,param: String): Option[String] = {
    db.get(s"TWEETORRO:USERS:$user:LOGGED") match {
      case Some("true") =>
        db.get(s"TWEETORRO:USERS:$user:$param")
      case _ => 
        None
    }
  }
  
  def modifyRemoteProfile(user: String, param: String, value: String): Boolean = {
    db.set(s"TWEETORRO:USERS:$user:$param", value) match {
      case true => 
        true
      case false => 
        println("Some error ocurred setting the value at the server. =( ")
        println("This shouldnt never ever ever ever happen... So if you see this line you are allowed to scream and panic")
        false
    }    
  }
  
  def follow(user:String, userF: String): Boolean = {
    db.get(s"TWEETORRO:USERS:$user:LOGGED") match {
      case Some("true") =>
        db.lpush(s"TWEETORRO:USERS:$user:FOLLOWING", userF)
        db.lpush(s"TWEETORRO:USERS:$userF:FOLLOWERS", user)
        val listTweetsFollow = db.lrange(s"TWEETORRO:USERS:$userF:TWEETS",0,-1)
        listTweetsFollow.getOrElse(List()).flatten.map( 
          db.lpush(s"TWEETORRO:USERS:$user:TWEETS", _))
        true
      case _ => 
        false
    }
  }

  def unfollow(userLogged: String, userF: String): Boolean = {
    db.get(s"TWEETORRO:USERS:$userLogged:LOGGED") match {
      case Some("true") =>
        db.lrem(s"TWEETORRO:USERS:$userLogged:FOLLOWING", 1, userF)
        db.lrem(s"TWEETORRO:USERS:$userF:FOLLOWERS", 1, userLogged)
        val listTweets = db.lrange(s"TWEETORRO:USERS:$userF:TWEETS",0,-1)
        listTweets.getOrElse(List()).flatten.map ( 
          db.lrem(s"TWEETORRO:USERS:$userLogged:TWEETS", 1, _))
        true
      case _ =>
        println("Usuario no está loggeado")
        false
    }
  }

}

object Server {
  def main(args: Array[String]): Unit = {
    
    try {
      val server: ServerTrait = new Server
      val stub = UnicastRemoteObject.exportObject(server,0).asInstanceOf[ServerTrait]
      val registry = LocateRegistry.createRegistry(1099)
      registry.rebind("tweetorro", stub)
      println("Server ready :D")
      println("")
    }
    catch {
      case e: Exception => e printStackTrace
    }
    
  }
}