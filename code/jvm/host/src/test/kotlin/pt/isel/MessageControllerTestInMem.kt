package pt.isel

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("inMem")
class MessageControllerTestInMem : AbstractMessageControllerTest()
