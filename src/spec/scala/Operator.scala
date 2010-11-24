import org.specs._
import org.specs.runner._
import org.specs.matcher._
import org.specs.mock._

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.modules.{UserMask, Operator}

object StringSpecs extends Specification with JMocker with ClassMocker {

  val bot = mock[Grouphug]
  val handler = mock[ModuleHandler]

  "Operator module" should {
    expect {
      one(handler).addJoinListener()
      one(handler).addNickChangeListener()
    }
    val operator = new Operator(handler)
    /*
    "match user on nick" in {
      UserMask("murr4y", "0", "1").equals(UserMask("murr4y", "", "")) must beTrue
    }
    "op Krashk in #grouphugs" in {
      operator.shouldOp("#grouphugs", "Krashk") must beTrue
    }
    "not op zokum in #grouphugs" in {
      operator.shouldOp("#grouphugs", "zokum") must beFalse
    }
    */
  }
}
