<script>
        // Timer State
        let timer;
        let timeLeft;
        let isRunning = false;
        let currentMode = 'pomodoro';
        let activeTaskId = null;
        let sessionStartTime = null;

        // DOM Elements
        const timerDisplay = document.getElementById('timer-display');
        const startBtn = document.getElementById('start-btn');
        const pauseBtn = document.getElementById('pause-btn');
        const resetBtn = document.getElementById('reset-btn');
        const modeButtons = document.querySelectorAll('.mode-btn');
        const pomodoroTimeInput = document.getElementById('pomodoro-time');
        const shortBreakTimeInput = document.getElementById('short-break-time');
        const longBreakTimeInput = document.getElementById('long-break-time');
        // Stats
        const focusTimeEl = document.getElementById('focus-time');
        const tasksCompletedEl = document.getElementById('tasks-completed');
        const totalSessionsEl = document.getElementById('total-sessions');
        const dailyGoalEl = document.getElementById('daily-goal');
        // Tasks
        const taskInput = document.getElementById('task-input');
        const addTaskBtn = document.getElementById('add-task-btn');
        const taskList = document.getElementById('task-list');
        // Dark Mode
        const body = document.body;
        const darkModeToggle = document.getElementById('dark-mode-toggle');

        // Initialization
        function initTimer() {
            loadSettings();
            loadStats();
            loadTasks();
            updateTimer();
            updateQuote();
            updateDarkMode();
        }

        // SETTINGS
        function loadSettings() {
            pomodoroTimeInput.value = +localStorage.getItem('pomodoroTime') || 25;
            shortBreakTimeInput.value = +localStorage.getItem('shortBreakTime') || 5;
            longBreakTimeInput.value = +localStorage.getItem('longBreakTime') || 15;
            document.getElementById('daily-goal-time').value = +localStorage.getItem('dailyGoalTime') || 120;
        }
        document.getElementById('save-settings-btn').onclick = function() {
            localStorage.setItem('pomodoroTime', pomodoroTimeInput.value);
            localStorage.setItem('shortBreakTime', shortBreakTimeInput.value);
            localStorage.setItem('longBreakTime', longBreakTimeInput.value);
            localStorage.setItem('dailyGoalTime', document.getElementById('daily-goal-time').value);
            updateTimer();
        };

        // DARK MODE
        darkModeToggle.onclick = function() {
            body.classList.toggle('dark-mode');
            document.querySelectorAll('.timer-container, .timer-display').forEach(el=>
                el.classList.toggle('dark-mode'));
            localStorage.setItem('darkMode', body.classList.contains('dark-mode'));
        }
        function updateDarkMode() {
            if (localStorage.getItem('darkMode')==='true') darkModeToggle.click();
        }

        // TIMER MODES
        modeButtons.forEach(btn => {
            btn.onclick = function() {
                switchMode(this.dataset.mode);
            };
        });
        function switchMode(mode) {
            currentMode = mode;
            modeButtons.forEach(btn =>
                btn.classList.toggle('active', btn.dataset.mode === mode)
            );
            updateTimer();
            resetTimer();
        }
        function updateTimer() {
            let minutes = {
                'pomodoro': +pomodoroTimeInput.value,
                'short-break': +shortBreakTimeInput.value,
                'long-break': +longBreakTimeInput.value
            }[currentMode] || 25;
            timeLeft = minutes * 60;
            updateDisplay();
        }
        function updateDisplay() {
            const minutes = Math.floor(timeLeft / 60);
            const seconds = timeLeft % 60;
            timerDisplay.textContent =
                ${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')};
        }

        // TIMER ACTIONS
        startBtn.onclick = function() {
            if (isRunning) return;
            isRunning = true;
            startBtn.disabled = true;
            pauseBtn.disabled = false;
            sessionStartTime = Date.now();
            timer = setInterval(() => {
                timeLeft--;
                updateDisplay();
                if (timeLeft <= 0) {
                    clearInterval(timer);
                    isRunning = false;
                    timerComplete();
                }
            }, 1000);
        };
        pauseBtn.onclick = function() {
            if (!isRunning) return;
            clearInterval(timer);
            isRunning = false;
            startBtn.disabled = false;
            pauseBtn.disabled = true;
        };
        resetBtn.onclick = function() {
            pauseTimer();
            updateTimer();
        };
        function pauseTimer() {
            if (timer) clearInterval(timer);
            isRunning = false;
            startBtn.disabled = false;
            pauseBtn.disabled = true;
        }
        // On timer completes a session
        function timerComplete() {
            playNotificationSound();
            updateStats();
            showNotification();
            if (currentMode === 'pomodoro') {
                logTimeForTask();
                let sessions = +localStorage.getItem('sessionsCompleted') || 0;
                sessions++;
                localStorage.setItem('sessionsCompleted', sessions);
                if (sessions % 4 === 0) switchMode('long-break');
                else switchMode('short-break');
            } else {
                switchMode('pomodoro');
            }
            updateStats();
        }
        function logTimeForTask() {
            if (activeTaskId !== null) {
                let taskObj = getTaskById(activeTaskId);
                let spent = taskObj ? (taskObj.timeSpent || 0) : 0;
                let sessionDuration = Math.max((Date.now() - sessionStartTime) / 1000, 0);
                updateTaskTimeSpent(activeTaskId, spent + sessionDuration);
            }
            sessionStartTime = null;
        }
        // TASKS
        function getTaskById(id) {
            let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
            return tasks.find(t => t.id === id);
        }
        function updateTaskTimeSpent(id, time) {
            let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
            tasks = tasks.map(t => t.id === id ? { ...t, timeSpent: time } : t);
            localStorage.setItem('tasks', JSON.stringify(tasks));
            renderTasks();
        }
        addTaskBtn.onclick = function() {
            const name = taskInput.value.trim();
            if (!name) return;
            let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
            let task = { id: Date.now(), name, completed: false, timeSpent: 0 };
            tasks.push(task);
            localStorage.setItem('tasks', JSON.stringify(tasks));
            taskInput.value = '';
            renderTasks();
        };
        function renderTasks() {
            let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
            taskList.innerHTML = '';
            tasks.forEach(task => {
                let li = document.createElement('li');
                li.className = 'task-item';
                li.innerHTML =
                    `<span class="task-info">
                        <input type="checkbox" class="task-checkbox" data-id="${task.id}" ${task.completed ? 'checked' : ''}>
                        <span>${task.name}</span>
                    </span>
                    <span class="task-time">${formatMinutes(task.timeSpent)}</span>`;
                // Set active highlight
                if (activeTaskId === task.id) li.style.fontWeight = 'bold';
                li.onclick = function(e) {
                    if (e.target.tagName === 'INPUT') return;
                    activeTaskId = task.id;
                    renderTasks();
                };
                li.querySelector('.task-checkbox').onchange = function() {
                    markTaskCompleted(task.id, this.checked);
                };
                taskList.appendChild(li);
            });
        }
        function markTaskCompleted(id, completed) {
            let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
            tasks = tasks.map(t => t.id === id ? { ...t, completed } : t);
            localStorage.setItem('tasks', JSON.stringify(tasks));
            updateStats();
            renderTasks();
        }
        function loadTasks() {
            renderTasks();
            if (!activeTaskId) {
                let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
                if (tasks.length) activeTaskId = tasks[0].id;
            }
            renderTasks();
        }
        function formatMinutes(sec) {
            let min = Math.round(sec/60);
            return min > 0 ? ${min} min : '';
        }
        // STATS
        function updateStats() {
            let totalFocus = 0;
            let completed = 0;
            let sessions = +localStorage.getItem('sessionsCompleted') || 0;
            let tasks = JSON.parse(localStorage.getItem('tasks') || '[]');
            tasks.forEach(t => {
                totalFocus += (t.timeSpent || 0);
                if (t.completed) completed++;
            });
            focusTimeEl.textContent = ${Math.round(totalFocus/60)} min;
            tasksCompletedEl.textContent = completed.toString();
            totalSessionsEl.textContent = sessions.toString();
            let dailyGoalMin = +localStorage.getItem('dailyGoalTime') || 120;
            let percent = Math.min(100, Math.round((totalFocus/60/dailyGoalMin)*100));
            dailyGoalEl.textContent = ${percent}%;
            updateLeaderboard(totalFocus);
        }
        function loadStats() { updateStats(); }
        function updateLeaderboard(totalFocus) {
            document.getElementById('user-score').textContent = ${Math.round(totalFocus/60)} min;
        }
        // QUOTE FUNCTION
        function updateQuote() {
            const quotes = [
                ["The secret of getting ahead is getting started.", "Mark Twain"],
                ["Genius is 1% inspiration, 99% perspiration.", "Thomas Edison"],
                ["It's not about having time. It's about making time.", "Unknown"],
                ["Productivity is being able to do things that you were never able to do before.", "Franz Kafka"],
                ["Success is the sum of small efforts repeated day in and day out.", "Robert Collier"]
            ];
            const q = quotes[Math.floor(Math.random() * quotes.length)];
            document.getElementById('quote-text').textContent = "${q[0]}";
            document.getElementById('quote-author').textContent = - ${q};
        }
        // NOTIFICATION
        function playNotificationSound() {
            // Optionally play a ding or chime
            // let audio = new Audio('https://actions.google.com/sounds/v1/alarms/digital_watch_alarm_long.ogg');
            // audio.play();
        }
        function showNotification() {
            if (document.hidden && "Notification" in window) {
                if (Notification.permission === "granted") {
                    new Notification("FocusFlow", { body: "Session complete!" });
                } else if (Notification.permission !== "denied") {
                    Notification.requestPermission().then(function (permission) {
                        if (permission === "granted") {
                            new Notification("FocusFlow", { body: "Session complete!" });
                        }
                    });
                }
            }
        }
        // INIT
        initTimer();
    </script>
