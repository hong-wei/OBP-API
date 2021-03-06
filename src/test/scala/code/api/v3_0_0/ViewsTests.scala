/**
Open Bank Project - API
Copyright (C) 2011-2016, TESOBE Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE Ltd
Osloerstrasse 16/17
Berlin 13359, Germany

  This product includes software developed at
  TESOBE (http://www.tesobe.com/)
  by
  Simon Redfern : simon AT tesobe DOT com
  Stefan Bethge : stefan AT tesobe DOT com
  Everett Sochowski : everett AT tesobe DOT com
  Ayoub Benali: ayoub AT tesobe DOT com

  */
package code.api.v3_0_0

import _root_.net.liftweb.json.Serialization.write
import code.api.ErrorMessage
import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON._
import code.api.util.APIUtil.OAuth._
import code.api.v1_2_1.APIInfoJSON
import code.api.v2_2_0.{ViewJSONV220, ViewsJSONV220}
import code.model.{CreateViewJson, UpdateViewJSON}
import code.setup.APIResponse
import net.liftweb.util.Helpers._

class ViewsTests extends V300ServerSetup {
  
  val postBodyViewJson = createViewJson
  
  def getAccountViews(bankId : String, accountId : String, consumerAndToken: Option[(Consumer, Token)]): APIResponse = {
    val request = v3_0Request / "banks" / bankId / "accounts" / accountId / "views" <@(consumerAndToken)
    makeGetRequest(request)
  }

  def postView(bankId: String, accountId: String, view: CreateViewJson, consumerAndToken: Option[(Consumer, Token)]): APIResponse = {
    val request = (v3_0Request / "banks" / bankId / "accounts" / accountId / "views").POST <@(consumerAndToken)
    makePostRequest(request, write(view))
  }

  def putView(bankId: String, accountId: String, viewId : String, view: UpdateViewJSON, consumerAndToken: Option[(Consumer, Token)]): APIResponse = {
    val request = (v3_0Request / "banks" / bankId / "accounts" / accountId / "views" / viewId).PUT <@(consumerAndToken)
    makePutRequest(request, write(view))
  }

/************************ the tests ************************/
  feature("/root"){
    scenario("The root of the API") {
      Given("Nothing, this one always is working ")
      val httpResponse = getAPIInfo
      Then("we should get a 200 ok code")
      httpResponse.code should equal (200)
      val apiInfo = httpResponse.body.extract[APIInfoJSON]
      apiInfo.version should equal ("3.0.0")
    }
  }

  feature("getViewsForBankAccount - V300"){
    scenario("All requirements") {
      Given("The BANK_ID, ACCOUNT_ID and Login User")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      val loginedUser= user1
      
      When("The request is sent")
      val httpResponse = getAccountViews(bankId, bankAccountId, loginedUser)
      
      Then("we should get a 200 ok code and the right json body")
      httpResponse.code should equal (200)
      httpResponse.body.extract[ViewsJsonV300]
    }

    scenario("no Auth") {
      Given("BANK_ID, ACCOUNT_ID, but no Login User")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      When("the request is sent")
      val reply = getAccountViews(bankId, bankAccountId, None)
      Then("we should get a 400 code")
      reply.code should equal (400)
      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }

    scenario("No Views") {
      Given("BANK_ID, ACCOUNT_ID, Login User but no views")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      val loginUserNoRoles = user3
      When("the request is sent")
      val reply = getAccountViews(bankId, bankAccountId, user3)
      Then("we should get a 400 code")
      reply.code should equal (400)
      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }
  }
  
  feature("createViewForBankAccount - V300"){
    scenario("all requirements") {
      Given("The BANK_ID, ACCOUNT_ID, Login User and postViewBody")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      val postViewBody = postBodyViewJson
      val loginedUser = user1
      val viewsBefore = getAccountViews(bankId, bankAccountId, loginedUser).body.extract[ViewsJsonV300].views

      When("the request is sent")
      val reply = postView(bankId, bankAccountId, postViewBody, loginedUser)
      Then("we should get a 201 code")
      reply.code should equal (201)
      reply.body.extract[ViewJSONV220]
      And("we should get a new view")
      val viewsAfter = getAccountViews(bankId, bankAccountId, loginedUser).body.extract[ViewsJsonV300].views
      viewsBefore.size should equal (viewsAfter.size -1)
    }

    scenario("no Auth") {
      Given("The BANK_ID, ACCOUNT_ID, No Login user")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      When("the request is sent")
      val reply = postView(bankId, bankAccountId, postBodyViewJson, None)
      Then("we should get a 400 code")
      reply.code should equal (400)
      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }

    scenario("no views") {
      Given("The BANK_ID, ACCOUNT_ID, Login user, no views")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      When("the request is sent")
      val reply = postView(bankId, bankAccountId, postBodyViewJson, user3)
      Then("we should get a 400 code")
      reply.code should equal (400)
      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }

    scenario("no existing account") {
      Given("The BANK_ID, wrong ACCOUNT_ID, Login user, views")
      val bankId = randomBankId
      When("the request is sent")
      val reply = postView(bankId, randomString(3), postBodyViewJson, user1)
      Then("we should get a 400 code")
      reply.code should equal (400)
      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }

    scenario("view already exists") {
      Given("The BANK_ID, ACCOUNT_ID, Login user, views")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      postView(bankId, bankAccountId, postBodyViewJson, user1)
      When("the request is sent")
      val reply = postView(bankId, bankAccountId, postBodyViewJson, user1)
      Then("we should get a 400 code")
      reply.code should equal (400)
      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }
  }

  feature("updateViewForBankAccount - v3.0.0") {

    val updatedViewDescription = "aloha"
    val updatedAliasToUse = "public"
    val allowedActions = List("can_see_images", "can_delete_comment")

    def viewUpdateJson(originalView : ViewJsonV300) = {
      //it's not perfect, assumes too much about originalView (i.e. randomView(true, ""))
      UpdateViewJSON(
        description = updatedViewDescription,
        is_public = !originalView.is_public,
        which_alias_to_use = updatedAliasToUse,
        hide_metadata_if_alias_used = !originalView.hide_metadata_if_alias_used,
        allowed_actions = allowedActions
      )
    }

    def someViewUpdateJson() = {
      UpdateViewJSON(
        description = updatedViewDescription,
        is_public = true,
        which_alias_to_use = updatedAliasToUse,
        hide_metadata_if_alias_used = true,
        allowed_actions = allowedActions
      )
    }

    scenario("we will update a view on a bank account") {
      Given("A view exists")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      val creationReply = postView(bankId, bankAccountId, postBodyViewJson, user1)
      creationReply.code should equal (201)
      val createdView : ViewJsonV300 = creationReply.body.extract[ViewJsonV300]
      createdView.can_see_images should equal(true)
      createdView.can_delete_comment should equal(true)
      createdView.can_delete_physical_location should equal(true)
      createdView.can_edit_owner_comment should equal(true)
      createdView.description should not equal(updatedViewDescription)
      createdView.is_public should equal(true)
      createdView.hide_metadata_if_alias_used should equal(false)

      When("We use a valid access token and valid put json")
      val reply = putView(bankId, bankAccountId, createdView.id, viewUpdateJson(createdView), user1)
      Then("We should get back the updated view")
      reply.code should equal (200)
      val updatedView = reply.body.extract[ViewJsonV300]
      updatedView.can_see_images should equal(true)
      updatedView.can_delete_comment should equal(true)
      updatedView.can_delete_physical_location should equal(false)
      updatedView.can_edit_owner_comment should equal(false)
      updatedView.description should equal(updatedViewDescription)
      updatedView.is_public should equal(false)
      updatedView.hide_metadata_if_alias_used should equal(true)
    }

    scenario("we will not update a view that doesn't exist") {
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)

      Given("a view does not exist")
      val nonExistantViewId = "asdfasdfasdfasdfasdf"
      val getReply = getAccountViews(bankId, bankAccountId, user1)
      getReply.code should equal (200)
      val views : ViewsJSONV220 = getReply.body.extract[ViewsJSONV220]
      views.views.foreach(v => v.id should not equal(nonExistantViewId))

      When("we try to update that view")
      val reply = putView(bankId, bankAccountId, nonExistantViewId, someViewUpdateJson(), user1)
      Then("We should get a 404")
      reply.code should equal(404)
    }

    scenario("We will not update a view on a bank account due to missing token") {
      Given("A view exists")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      val creationReply = postView(bankId, bankAccountId, postBodyViewJson, user1)
      creationReply.code should equal (201)
      val createdView : ViewJsonV300 = creationReply.body.extract[ViewJsonV300]

      When("we don't use an access token")
      val reply = putView(bankId, bankAccountId, createdView.id, viewUpdateJson(createdView), None)
      Then("we should get a 400")
      reply.code should equal(400)

      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }

    scenario("we will not update a view on a bank account due to insufficient privileges") {
      Given("A view exists")
      val bankId = randomBankId
      val bankAccountId = randomPrivateAccountId(bankId)
      val creationReply = postView(bankId, bankAccountId, postBodyViewJson, user1)
      creationReply.code should equal (201)
      val createdView : ViewJsonV300 = creationReply.body.extract[ViewJsonV300]

      When("we try to update a view without having sufficient privileges to do so")
      val reply = putView(bankId, bankAccountId, createdView.id, viewUpdateJson(createdView), user3)
      Then("we should get a 400")
      reply.code should equal(400)

      And("we should get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty should equal (true)
    }
  }

}
