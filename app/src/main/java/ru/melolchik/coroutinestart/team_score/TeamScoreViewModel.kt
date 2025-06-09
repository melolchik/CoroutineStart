package ru.melolchik.coroutinestart.team_score

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TeamScoreViewModel : ViewModel() {

    private val _state = MutableStateFlow<TeamScoreState>(TeamScoreState.Game(0, 0));
    val state = _state.asStateFlow()

    fun increaseScore(team: Team) {


        val currentState = state.value
        if (currentState is TeamScoreState.Game) {
            if (team == Team.TEAM_1) {
                val oldValue = currentState.score1
                val newValue = oldValue + 1
                _state.value = (currentState.copy(score1 = newValue))
                if (newValue >= WINNER_SCORE) {
                    _state.value =
                        TeamScoreState.Winner(
                            winnerTeam = Team.TEAM_1,
                            newValue,
                            currentState.score2
                        )

                }
            } else {
                val oldValue = currentState.score2
                val newValue = oldValue + 1
                _state.value = (currentState.copy(score2 = newValue))
                if (newValue >= WINNER_SCORE) {
                    _state.value =
                        TeamScoreState.Winner(
                            winnerTeam = Team.TEAM_2,
                            currentState.score1,
                            newValue
                        )

                }
            }
        }
    }

    companion object {
        private const val WINNER_SCORE = 7
    }
}