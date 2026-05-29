-- Insert sample users first
INSERT IGNORE INTO users (id, name, email, password, xp, level, streak_days, created_at)
VALUES
(1, 'Ravi Kumar', 'ravi@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 150, 'Newcomer', 2, NOW()),
(2, 'Priya Singh', 'priya@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 320, 'Thinker', 5, NOW()),
(3, 'Arjun Mehta', 'arjun@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 780, 'Innovator', 8, NOW()),
(4, 'Sneha Reddy', 'sneha@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1200, 'Visionary', 12, NOW()),
(5, 'Admin User', 'admin@ideaforge.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 0, 'Newcomer', 0, NOW());

-- All passwords above are: test1234

-- Insert sample startup ideas
INSERT IGNORE INTO ideas (id, title, pitch, problem, target_audience, category, user_id, traction_score, vote_count, comment_count, battle_status, visibility, agreed_to_terms, is_plagiarised, created_at)
VALUES
(1, 'FarmConnect', 'A platform connecting farmers directly to urban consumers eliminating middlemen', 'Farmers get very low prices while consumers pay high prices due to middlemen in the supply chain', 'Farmers and urban grocery buyers', 'FinTech', 1, 85.5, 28, 6, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 5 DAY),
(2, 'MediQueue', 'AI-powered hospital queue management to reduce patient waiting time', 'Patients wait 2-3 hours in hospitals with no visibility on queue status', 'Hospitals and patients', 'HealthTech', 2, 72.0, 21, 4, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 4 DAY),
(3, 'SkillBridge', 'A micro-internship platform connecting college students with startups for 2-4 week projects', 'Students lack real work experience and startups need affordable short-term talent', 'College students and early-stage startups', 'EdTech', 3, 91.0, 35, 8, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 3 DAY),
(4, 'GreenMile', 'Carbon footprint tracker for daily commuters with rewards for eco-friendly choices', 'People have no easy way to track or reduce their daily carbon emissions', 'Urban commuters aged 18-35', 'SaaS', 4, 65.0, 18, 3, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 2 DAY),
(5, 'RentEase', 'A verified rental platform for PG and apartment hunting with virtual tours', 'Students and working professionals waste weeks finding trustworthy rentals in new cities', 'Students and young professionals relocating to new cities', 'SaaS', 1, 78.0, 24, 5, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 1 DAY),
(6, 'PetCare24', 'On-demand veterinary consultation app for pet owners available 24/7', 'Pet owners panic during emergencies and cannot find vets quickly especially at night', 'Pet owners in urban areas', 'HealthTech', 2, 55.0, 15, 2, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 6 DAY),
(7, 'LocalBite', 'Hyperlocal food discovery app for authentic home-cooked meals from home chefs', 'People crave authentic homemade food but restaurant food feels industrial and overpriced', 'Working professionals and food lovers', 'SaaS', 3, 88.0, 30, 7, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 7 DAY),
(8, 'StudyCircle', 'Peer-to-peer study group platform with AI-generated quizzes from uploaded notes', 'Students study alone and lose motivation — group study has no good digital platform', 'School and college students', 'EdTech', 4, 69.0, 20, 4, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 8 DAY),
(9, 'FreelanceShield', 'Escrow payment protection platform for Indian freelancers to prevent payment fraud', 'Freelancers frequently get cheated by clients who disappear after work is delivered', 'Freelancers and independent consultants in India', 'FinTech', 1, 95.0, 40, 10, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 9 DAY),
(10, 'ElderLink', 'Companionship and daily task assistance app connecting elderly people with trained volunteers', 'Elderly people living alone feel isolated and struggle with daily tasks like grocery ordering', 'Senior citizens and their families', 'HealthTech', 2, 60.0, 17, 3, 'NONE', 'PUBLIC', true, false, NOW() - INTERVAL 10 DAY);

-- Insert ownership certificates for all ideas
INSERT IGNORE INTO idea_certificates (idea_id, certificate_code, issued_at)
VALUES
(1, UUID(), NOW() - INTERVAL 5 DAY),
(2, UUID(), NOW() - INTERVAL 4 DAY),
(3, UUID(), NOW() - INTERVAL 3 DAY),
(4, UUID(), NOW() - INTERVAL 2 DAY),
(5, UUID(), NOW() - INTERVAL 1 DAY),
(6, UUID(), NOW() - INTERVAL 6 DAY),
(7, UUID(), NOW() - INTERVAL 7 DAY),
(8, UUID(), NOW() - INTERVAL 8 DAY),
(9, UUID(), NOW() - INTERVAL 9 DAY),
(10, UUID(), NOW() - INTERVAL 10 DAY);

-- Insert some sample votes
INSERT IGNORE INTO votes (idea_id, user_id, type, created_at) VALUES
(1, 2, 'UP', NOW()), (1, 3, 'UP', NOW()), (1, 4, 'UP', NOW()),
(3, 1, 'UP', NOW()), (3, 2, 'UP', NOW()), (3, 4, 'UP', NOW()),
(9, 1, 'UP', NOW()), (9, 3, 'UP', NOW()), (9, 4, 'UP', NOW()),
(7, 1, 'UP', NOW()), (7, 2, 'UP', NOW()), (7, 4, 'UP', NOW());

-- Insert some sample comments
INSERT IGNORE INTO comments (idea_id, user_id, content, tag, answered, created_at) VALUES
(1, 2, 'This is exactly what farmers in Tamil Nadu need!', 'SUPPORT', false, NOW()),
(1, 3, 'How will you handle logistics and cold storage?', 'QUESTION', false, NOW()),
(3, 1, 'SkillBridge would have helped me a lot during college', 'SUPPORT', false, NOW()),
(9, 2, 'FreelanceShield is a must-have — I have been cheated twice', 'SUPPORT', false, NOW()),
(9, 4, 'What is your revenue model here?', 'CHALLENGE', false, NOW()),
(7, 1, 'LocalBite is a brilliant idea — trust factor is key', 'SUPPORT', false, NOW());