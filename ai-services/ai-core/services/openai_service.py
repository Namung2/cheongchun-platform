import openai
import os
from typing import List, AsyncGenerator, Dict, Optional
import asyncio
import json
from .backend_service import BackendService
from .conversation_analyzer import ConversationAnalyzer

class OpenAIService:
    def __init__(self):
        self.client = openai.AsyncOpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        self.model = "gpt-4o-mini"
        self.backend_service = BackendService()
        self.conversation_analyzer = ConversationAnalyzer()
        
        # 간결한 시니어 맞춤 시스템 프롬프트 (토큰 절약)
        self.base_system_prompt = """
시니어용 AI 도우미입니다. 존댓말로 간결하고 따뜻하게 답변하며, 의료/법률 조언시 전문가 상담을 권유합니다.
        """.strip()
    
    async def stream_chat_response(
        self, 
        message: str, 
        chat_history: List[dict] = None, 
        user_token: str = None,
        user_id: int = None
    ) -> AsyncGenerator[str, None]:
        """개인화된 스트리밍 채팅 응답 생성"""
        try:
            # 사용자 맞춤 시스템 프롬프트 생성
            system_prompt = await self._get_personalized_system_prompt(user_token, user_id)
            
            messages = [{"role": "system", "content": system_prompt}]
            
            # 채팅 히스토리 추가 (최근 10개 메시지만)
            if chat_history:
                messages.extend(chat_history[-10:])
            
            # 새로운 사용자 메시지 추가
            messages.append({"role": "user", "content": message})
            
            # OpenAI API 스트리밍 호출
            stream = await self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                stream=True,
                max_tokens=300,
                temperature=0.3
            )
            
            async for chunk in stream:
                if chunk.choices[0].delta.content is not None:
                    yield chunk.choices[0].delta.content
                    
        except Exception as e:
            error_msg = "죄송합니다. 현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."
            yield error_msg
    
    async def get_chat_response(self, message: str, chat_history: List[dict] = None) -> str:
        """일반 채팅 응답 생성 (비스트리밍)"""
        try:
            messages = [{"role": "system", "content": self.base_system_prompt}]
            
            if chat_history:
                messages.extend(chat_history[-10:])
            
            messages.append({"role": "user", "content": message})
            
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                max_tokens=300,
                temperature=0.3
            )
            
            return response.choices[0].message.content
            
        except Exception as e:
            return "죄송합니다. 현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."
    
    async def _get_personalized_system_prompt(self, user_token: str = None, user_id: int = None) -> str:
        """사용자 맞춤 시스템 프롬프트 생성"""
        personalized_prompt = self.base_system_prompt
        
        try:
            if user_token:
                # 사용자 AI 프로필 조회
                user_profile = await self.backend_service.get_user_ai_profile(user_token)
                if user_profile:
                    personalized_prompt += await self._add_personalization_context(user_profile)
            
            elif user_id:
                # 사용자 인사이트 조회
                user_insights = await self.backend_service.get_user_insights(user_id)
                if user_insights:
                    personalized_prompt += await self._add_insights_context(user_insights)
                    
        except Exception as e:
            # 개인화 실패시 기본 프롬프트 사용
            pass
            
        return personalized_prompt
    
    async def _add_personalization_context(self, user_profile: Dict) -> str:
        """사용자 프로필 기반 개인화 컨텍스트 추가"""
        context = "\n\n=== 사용자 맞춤 정보 ===\n"
        
        # 나이대별 맞춤
        if user_profile.get("ageGroup"):
            age_group = user_profile["ageGroup"]
            context += f"사용자 나이대: {age_group}\n"
            
            if "75+" in age_group:
                context += "- 더욱 천천히, 자세히 설명해주세요\n"
                context += "- 기술적 용어는 특히 쉽게 풀어서 설명해주세요\n"
        
        # 건강 프로필
        if user_profile.get("healthProfile"):
            health_info = user_profile["healthProfile"]
            if isinstance(health_info, dict):
                concerns = health_info.get("concerns", [])
                if concerns:
                    context += f"- 주요 건강 관심사: {', '.join(concerns)}\n"
        
        # 관심사
        if user_profile.get("interests"):
            interests = user_profile["interests"]
            if interests:
                context += f"- 관심 분야: {', '.join(interests)}\n"
                context += "- 이러한 관심사와 연관된 대화를 선호하실 수 있습니다\n"
        
        # 대화 스타일
        conversation_style = user_profile.get("conversationStyle", "formal")
        if conversation_style == "casual":
            context += "- 좀 더 편안하고 친근한 대화를 선호합니다\n"
        else:
            context += "- 정중하고 공손한 대화를 선호합니다\n"
        
        # 최근 대화 요약
        if user_profile.get("lastSummary"):
            context += f"- 최근 대화 요약: {user_profile['lastSummary']}\n"
        
        return context
    
    async def _add_insights_context(self, user_insights: Dict) -> str:
        """사용자 인사이트 기반 컨텍스트 추가"""
        context = "\n\n=== 사용자 패턴 분석 ===\n"
        
        # 주요 관심사
        if user_insights.get("topInterests"):
            interests = user_insights["topInterests"][:3]  # 상위 3개만
            context += f"자주 대화하는 주제: {', '.join(interests)}\n"
        
        # 건강 관련 주제
        if user_insights.get("frequentHealthTopics"):
            health_topics = user_insights["frequentHealthTopics"][:3]
            context += f"건강 관련 주요 관심사: {', '.join(health_topics)}\n"
        
        # 스트레스 레벨 고려
        stress_level = user_insights.get("averageStressLevel", 5)
        if stress_level >= 7:
            context += "- 최근 스트레스 수준이 높으니 더욱 위로하는 톤으로 대화해주세요\n"
        elif stress_level <= 3:
            context += "- 평온한 상태이니 편안한 대화를 이어가주세요\n"
        
        # 추천 대화 스타일
        recommended_style = user_insights.get("recommendedConversationStyle")
        if recommended_style:
            context += f"- 추천 대화 방식: {recommended_style}\n"
        
        return context
    
    async def save_conversation_analysis(
        self, 
        user_id: int, 
        messages: List[Dict], 
        duration_minutes: int = 0
    ):
        """대화 종료 후 분석 결과 저장"""
        try:
            # 사용자 프로필 조회 (분석 개선용)
            user_insights = await self.backend_service.get_user_insights(user_id)
            user_profile = user_insights if user_insights else {}
            
            # 대화 분석 수행
            analysis = await self.conversation_analyzer.analyze_conversation(messages, user_profile)
            
            # 백엔드에 저장할 데이터 구성
            conversation_data = {
                "userId": user_id,
                "sessionTitle": analysis.get("session_title", "AI 대화"),
                "totalMessages": len(messages),
                "durationMinutes": duration_minutes,
                "messagesJson": json.dumps(messages, ensure_ascii=False),
                "mainTopics": analysis.get("main_topics", []),
                "healthMentions": analysis.get("health_mentions", []),
                "concernsDiscussed": analysis.get("concerns_discussed", []),
                "moodAnalysis": analysis.get("mood_analysis", "neutral"),
                "stressLevel": analysis.get("stress_level", 5),
                "conversationSummary": analysis.get("conversation_summary", ""),
                "keyInsights": analysis.get("key_insights", []),
                "aiRecommendations": analysis.get("ai_recommendations", [])
            }
            
            # 백엔드에 저장
            conversation_id = await self.backend_service.save_conversation(conversation_data)
            
            return {
                "conversation_id": conversation_id,
                "analysis": analysis,
                "saved": conversation_id is not None
            }
            
        except Exception as e:
            return {
                "conversation_id": None,
                "analysis": {},
                "saved": False,
                "error": str(e)
            }