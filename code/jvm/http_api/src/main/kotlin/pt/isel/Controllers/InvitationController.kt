package pt.isel.Controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pt.isel.*
import pt.isel.model.InvitationInput
import pt.isel.model.Problem


@RestController
@RequestMapping("/api/invitations")
class InvitationController(
    private val invitationService: InvitationService,
    private val userService: UserService
) {
    @GetMapping
    fun getAllInvitations(): ResponseEntity<List<Invitation>> {
        val invitations = invitationService.getAllInvitations()
        return ResponseEntity.ok(invitations)
    }

    @GetMapping("/{invitationId}")
    fun getInvitationById(
        @PathVariable invitationId: Int,
    ): ResponseEntity<Invitation> =
        when (val invitation = invitationService.getInvitationById(invitationId)) {
            is Success -> ResponseEntity.ok(invitation.value)
            is Failure -> ResponseEntity.notFound().build()
        }

    @PostMapping
    fun createInvitation(
        @RequestBody input: InvitationInput,
    ): ResponseEntity<Any> =
        when (val result = invitationService.createInvitation(input.inviterName, input.inviteeName, input.channelId, input.type)) {
            is Either.Right -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
            is Either.Left -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating invitation")
        }

    @PostMapping("/markAsUsed")
    fun markInvitationAsUsed(
        @RequestParam id: Int,
    ): ResponseEntity<*> =
        when (val result = invitationService.markInvitationAsUsed(id)) {
            is Either.Right -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Either.Left -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found")
        }

    @PostMapping("/accept")
    fun acceptInvitation(
        @RequestBody req: acceptInviteRequest,
    ): ResponseEntity<*> =
        when (val result = invitationService.acceptInvitation(req.username, req.invitationId, req.chId, req.permission)) {
            is Either.Right -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Either.Left -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found")
        }

    @PostMapping("/reject")
    fun rejectInvitation(
        @RequestBody req: rejectInviteRequest,
    ): ResponseEntity<*> =
        when (val result = invitationService.rejectInvitation(req.username, req.invitationId)) {
            is Either.Right -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Either.Left -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found")
        }

    @GetMapping("/byUser/{username}")
    fun getAllInvitationsByUserId(
        @PathVariable username: String,
    ): ResponseEntity<*> {
        return when (val result = invitationService.getAllinvitationsByUser(username)) {
            is Either.Right -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Either.Left -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found")
        }
    }

    @PostMapping("/{code}")
    fun acceptInvitationByCode(
        @PathVariable code: String,
        @RequestBody req: InvitationRegistration
    ): ResponseEntity<*> {
        val userResult = userService.createUser(req.username, req.password)
        if (userResult is Either.Left){
           return when (userResult.value) {
                is UserError.AlreadyUsedUsername ->
                    Problem.UsernameAlreadyInUse.response(HttpStatus.CONFLICT)
                UserError.InsecurePassword ->
                    Problem.InsecurePassword.response(HttpStatus.BAD_REQUEST)
            }
        }
        val result = invitationService.getInvitationByCode(code)
        if (result is Either.Right){
            invitationService.acceptInvitation(req.username, result.value.id, result.value.channelid, result.value.type)
            return ResponseEntity.status(HttpStatus.OK).body(result.value)
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found")
    }



}

