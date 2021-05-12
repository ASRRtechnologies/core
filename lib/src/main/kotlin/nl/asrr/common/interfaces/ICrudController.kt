package nl.asrr.common.interfaces

interface ICrudController {
    fun create()
    fun find(by: Any)
    fun update(updateDto: Any)
    fun delete(by: Any)
}