package proxy

import io.swagger.v3.oas.models.media.{ArraySchema, Content, MediaType, Schema}
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem, Paths}

import scala.collection.JavaConverters._

class ProxySwaggerBuilder(openAPI: OpenAPI) {
  val _openAPI: OpenAPI = openAPI
  _openAPI.getComponents.getSchemas.put("ProxySuccess", proxySuccessSchema)

  /**
   * Get proxy swagger openAPI
   * @return
   */
  def build(): OpenAPI = {
    _openAPI
  }

  /**
   * Success schema
   *
   * @return
   */
  private def successSchema: Schema[_] = {
    val successSchema: Schema[_] = new Schema()
    successSchema.setType("boolean")
    successSchema.setDescription("True if operation was successful")
    successSchema.setExample(true)

    successSchema
  }

  /**
   * Proxy success schema
   * @return
   */
  private def proxySuccessSchema: Schema[_] = {
    val schema: Schema[_] = new Schema()

    schema.setRequired(List[String]("success").asJava)
    schema.setType("object")

    schema.setProperties(Map[String, Schema[_]]("success" -> successSchema).asJava)

    schema
  }

  /**
   * Add pb to /mining/candidate
   */
  def addPB(): ProxySwaggerBuilder = {
    val pbSchema = new Schema()
    pbSchema.setType("Integer")
    pbSchema.setExample(9876543210L)
    _openAPI.getComponents.getSchemas.get("ExternalCandidateBlock").addProperties("pb", pbSchema)

    this
  }

  /**
   * Add share endpoint to swagger
   */
  def addShareEndpoint(): ProxySwaggerBuilder = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is valid")
      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is invalid")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ApiError")

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      response500.setDescription("Error")
      responses.setDefault(response500)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val security = new SecurityRequirement
      security.addList("ApiKeyAuth", "[api_key]")

      val op = new Operation
      op.addSecurityItem(security)
      op.setSummary("Submit share for current candidate")
      op.addTagsItem("mining")
      op.setRequestBody(openAPI.getPaths.get("/mining/solution").getPost.getRequestBody)
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(openAPI, "/mining/share", path, "/mining/rewardAddress")

    this
  }

  /**
   * Add pathItem after "after" key
   *
   * @param openAPI the object to add pathItem to
   * @param pathName name of the path
   * @param pathItem the pathItem to add
   * @param after the path that pathItem should be put after it
   */
  private def addPath(openAPI: OpenAPI, pathName: String, pathItem: PathItem, after: String): Unit = {
    // Reformat paths of openAPI
    val newPaths = new Paths
    openAPI.getPaths.forEach({
      case (name, value) =>
        newPaths.addPathItem(name, value)
        if (name == after)
          newPaths.addPathItem(pathName, pathItem)
    })
    openAPI.setPaths(newPaths)
  }

  /**
   * Add /proxy/config/reload to swagger
   */
  def addConfigReload(): ProxySwaggerBuilder = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Config reloaded")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 response
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.setDefault(response200)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val op = new Operation
      op.setSummary("Reload proxy config from the pool server")
      op.addTagsItem("proxy")
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(_openAPI, "/proxy/config/reload", path, "/info")

    this
  }

  /**
   * Add /proxy/status/reset to swagger
   */
  def addStatusReset(): ProxySwaggerBuilder = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Status reset successfully")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      response.setContent(jsonContentType(schema))

      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Reset status failed")

      val schema: Schema[_] = new Schema()

      schema.setRequired(List[String]("success", "message").asJava)
      schema.setType("object")

      val failSchema: Schema[_] = successSchema
      failSchema.setExample(false)

      val messageSchema: Schema[_] = new Schema()
      messageSchema.setType("string")
      messageSchema.setDescription("reason of failure in operation")
      messageSchema.setExample("Something happened")

      schema.setProperties(Map[String, Schema[_]]("success" -> failSchema, "message" -> messageSchema).asJava)

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      responses.setDefault(response500)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val op = new Operation
      op.setSummary("Reset status of proxy")
      op.addTagsItem("proxy")
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(_openAPI, "/proxy/status/reset", path, "/info")

    this
  }

  /**
   * Add /proxy/test to swagger
   */
  def addTest(): ProxySwaggerBuilder = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Proxy is working")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      response.setContent(jsonContentType(schema))

      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Exception happened when testing proxy")

      val schema: Schema[_] = new Schema()

      schema.setRequired(List[String]("success", "messages").asJava)
      schema.setType("object")

      val failSchema: Schema[_] = successSchema
      failSchema.setExample(false)

      val messageSchema: ArraySchema = new ArraySchema()
      messageSchema.setType("array")
      messageSchema.setDescription("List of reasons of failure")
      messageSchema.getItems

      messageSchema.setItems({
        val s: Schema[_] = new Schema()
        s.setType("string")
        s.setDescription("error messages during the test")
        s
      })
      schema.setProperties(Map[String, Schema[_]]("success" -> failSchema, "message" -> messageSchema).asJava)

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      responses.setDefault(response500)

      responses
    }

    // Create a get operation
    val getOperation: Operation = {
      val op = new Operation
      op.setSummary("Test proxy is working")
      op.addTagsItem("proxy")
      op.setResponses(APIResponses)

      op
    }

    // Add get Operation to paths
    val path: PathItem = new PathItem
    path.setGet(getOperation)

    addPath(_openAPI, "/proxy/test", path, "/info")

    this
  }

  /**
   * Return a json content type
   *
   * @param schema schema to use for content
   * @return
   */
  private def jsonContentType(schema: Schema[_]): Content = {
    val mediaType: MediaType = new MediaType
    mediaType.setSchema(schema)

    val content: Content = new Content
    content.addMediaType("application/json", mediaType)
  }

  /**
   * Add /proxy/mnemonic/load to swagger
   */
  def addLoadMnemonicEndpoint(): ProxySwaggerBuilder = {
    val response200: ApiResponse = {
      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      val response = new ApiResponse
      response.setDescription("Mnemonic has been loaded successfully")
      response.setContent(jsonContentType(schema))

      response
    }

    val response400: ApiResponse = {
      val failSchema: Schema[_] = successSchema
      failSchema.setExample(false)

      val messageSchema: Schema[_] = new Schema()
      messageSchema.setType("string")
      messageSchema.setDescription("reason of failure in operation")
      messageSchema.setExample("Password is wrong. Send the right one or remove mnemonic file.")

      val schema: Schema[_] = new Schema()
      schema.setRequired(List[String]("success", "message").asJava)
      schema.setType("object")
      schema.setProperties(Map[String, Schema[_]]("success" -> failSchema, "message" -> messageSchema).asJava)

      val response = new ApiResponse
      response.setDescription("Couldn't load mnemonic")
      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("400", response400)

      responses.setDefault(response400)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val passSchema: Schema[_] = new Schema
      passSchema.setType("string")
      passSchema.setDescription("Password of the mnemonic file")
      passSchema.setExample("My password")

      val schema: Schema[_] = new Schema
      schema.setProperties(Map[String, Schema[_]]("pass" -> passSchema).asJava)

      val reqBody = new RequestBody
      reqBody.setContent(jsonContentType(schema))

      val op = new Operation
      op.setSummary("Load mnemonic")
      op.addTagsItem("proxy")
      op.setRequestBody(reqBody)
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(_openAPI, "/proxy/mnemonic/load", path, "/info")

    this
  }

  /**
   * Add /proxy/mnemonic/save to swagger
   */
  def addSaveMnemonicEndpoint(): ProxySwaggerBuilder = {
    val response200: ApiResponse = {
      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      val response = new ApiResponse
      response.setDescription("Mnemonic has been saved into the file successfully")
      response.setContent(jsonContentType(schema))

      response
    }

    val response400: ApiResponse = {
      val failSchema: Schema[_] = successSchema
      failSchema.setExample(false)

      val messageSchema: Schema[_] = new Schema()
      messageSchema.setType("string")
      messageSchema.setDescription("reason of failure in operation")
      messageSchema.setExample("Mnemonic file already exists. You can remove the file if you want to change it.")

      val schema: Schema[_] = new Schema()
      schema.setRequired(List[String]("success", "message").asJava)
      schema.setType("object")
      schema.setProperties(Map[String, Schema[_]]("success" -> failSchema, "message" -> messageSchema).asJava)

      val response = new ApiResponse
      response.setDescription("Couldn't save mnemonic")
      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("400", response400)

      responses.setDefault(response400)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val passSchema: Schema[_] = new Schema
      passSchema.setType("string")
      passSchema.setDescription("Password to save mnemonic to file using it")
      passSchema.setExample("My password")

      val schema: Schema[_] = new Schema
      schema.setProperties(Map[String, Schema[_]]("pass" -> passSchema).asJava)

      val reqBody = new RequestBody
      reqBody.setContent(jsonContentType(schema))

      val op = new Operation
      op.setSummary("Save mnemonic to file using the password")
      op.addTagsItem("proxy")
      op.setRequestBody(reqBody)
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(_openAPI, "/proxy/mnemonic/save", path, "/info")

    this
  }
}
