package pt.isel

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("jdbi")
class InvitationControllerTestJdbi : AbstractInvitationControllerTest()
